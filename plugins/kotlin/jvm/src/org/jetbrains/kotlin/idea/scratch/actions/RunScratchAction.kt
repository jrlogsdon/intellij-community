// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbService
import com.intellij.task.ProjectTaskManager
import com.intellij.task.impl.ProjectTaskManagerImpl
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.SequentialScratchExecutor
import org.jetbrains.kotlin.idea.scratch.printDebugMessage
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.idea.scratch.LOG as log

class RunScratchActionK2 : ScratchAction(
    KotlinJvmBundle.getLazyMessage("scratch.run.button"), AllIcons.Actions.Execute
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val scratchFile = e.currentScratchFile ?: return
        val executor = scratchFile.k2ScratchExecutor ?: return
        val module = scratchFile.module

        ProjectTaskManagerImpl.putBuildOriginator(project, this.javaClass)

        if (module != null && scratchFile.options.isMakeBeforeRun) {
            ProjectTaskManager.getInstance(project).build(module).onSuccess { executionResult ->
                if (executionResult.isAborted || executionResult.hasErrors()) {
                    executor.errorOccurs(KotlinJvmBundle.message("there.were.compilation.errors.in.module.0", module.name))
                } else {
                    executor.execute()
                }
            }
        } else {
            executor.execute()
        }
    }

    class ExplainInfo(
        val variableName: String, val offsets: Pair<Int, Int>, val variableValue: Any?, val line: Int?
    ) {
        override fun toString(): String {
            return "ExplainInfo(variableName='$variableName', offsets=$offsets, variableValue=$variableValue, line=$line)"
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)


        val scratchFile = e.currentScratchFile ?: return

        e.presentation.isVisible = !scratchFile.options.isInteractiveMode

        e.presentation.isEnabled = !(ScratchCompilationSupport.isAnyInProgress() || scratchFile.options.isInteractiveMode)

        if (e.presentation.isEnabled) {
            e.presentation.text = templatePresentation.text
        } else {
            e.presentation.text = KotlinJvmBundle.message("other.scratch.file.execution.is.in.progress")
        }
    }
}

class RunScratchAction : ScratchAction(
    KotlinJvmBundle.getLazyMessage("scratch.run.button"), AllIcons.Actions.Execute
) {
    init {
        KeymapManager.getInstance().activeKeymap.getShortcuts("Kotlin.RunScratch").firstOrNull()?.let {
            templatePresentation.text += " (${KeymapUtil.getShortcutText(it)})"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val scratchFile = e.currentScratchFile ?: return
        Handler.doAction(scratchFile, false)
    }

    object Handler {
        fun doAction(scratchFile: ScratchFile, isAutoRun: Boolean) {
            val project = scratchFile.project
            val isRepl = scratchFile.options.isRepl
            val executor = (if (isRepl) scratchFile.replScratchExecutor else scratchFile.compilingScratchExecutor) ?: return

            log.printDebugMessage("Run Action: isRepl = $isRepl")

            fun executeScratch() {
                try {
                    if (isAutoRun && executor is SequentialScratchExecutor) {
                        executor.executeNew()
                    } else {
                        executor.execute()
                    }
                } catch (ex: Throwable) {
                    executor.errorOccurs(KotlinJvmBundle.message("exception.occurs.during.run.scratch.action"), ex, true)
                }
            }

            val isMakeBeforeRun = scratchFile.options.isMakeBeforeRun
            log.printDebugMessage("Run Action: isMakeBeforeRun = $isMakeBeforeRun")

            ScriptConfigurationManager.getInstance(project).cast<CompositeScriptConfigurationManager>()
                .updateScriptDependenciesIfNeeded(scratchFile.file)

            val module = scratchFile.module
            log.printDebugMessage("Run Action: module = ${module?.name}")

            if (!isAutoRun && module != null && isMakeBeforeRun) {
                ProjectTaskManagerImpl.putBuildOriginator(project, this.javaClass)
                ProjectTaskManager.getInstance(project).build(module).onSuccess { executionResult ->
                    if (executionResult.isAborted || executionResult.hasErrors()) {
                        executor.errorOccurs(KotlinJvmBundle.message("there.were.compilation.errors.in.module.0", module.name))
                    }

                    if (DumbService.isDumb(project)) {
                        DumbService.getInstance(project).smartInvokeLater {
                            executeScratch()
                        }
                    } else {
                        executeScratch()
                    }
                }
            } else {
                executeScratch()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val scratchFile = e.currentScratchFile ?: return

        e.presentation.isEnabled = !(ScratchCompilationSupport.isAnyInProgress() || scratchFile.options.isInteractiveMode)

        if (e.presentation.isEnabled) {
            e.presentation.text = templatePresentation.text
        } else {
            e.presentation.text = KotlinJvmBundle.message("other.scratch.file.execution.is.in.progress")
        }


        e.presentation.isVisible = !(ScratchCompilationSupport.isAnyInProgress() || scratchFile.options.isInteractiveMode)
    }
}