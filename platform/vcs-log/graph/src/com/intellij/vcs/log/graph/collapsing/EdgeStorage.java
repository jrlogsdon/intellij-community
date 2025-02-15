// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.vcs.log.graph.collapsing;

import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.utils.IntIntMultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class EdgeStorage {
  private static final int EDGE_TYPE_BITS = 4;
  private static final int EDGE_BITS_OFFSET = Integer.SIZE - EDGE_TYPE_BITS;
  private static final int COMPRESSED_NODE_ID_MASK = 0xffffffff >>> EDGE_TYPE_BITS;
  private static final int MAX_EDGE_TYPE_COUNT = 1 << EDGE_TYPE_BITS;
  private static final int MAX_NODE_ID = Integer.MAX_VALUE >> EDGE_TYPE_BITS;
  private static final int MIN_NODE_ID = Integer.MIN_VALUE >> EDGE_TYPE_BITS;

  public static final int NULL_ID = MIN_NODE_ID;

  private final @NotNull IntIntMultiMap myEdges = new IntIntMultiMap();

  public EdgeStorage() {
    assert GraphEdgeType.values().length <= MAX_EDGE_TYPE_COUNT;
  }

  public void createEdge(int mainNodeId, int additionId, GraphEdgeType edgeType) {
    if (edgeType.isNormalEdge()) {
      myEdges.putValue(mainNodeId, compressEdge(additionId, edgeType));
      myEdges.putValue(additionId, compressEdge(mainNodeId, edgeType));
    }
    else {
      myEdges.putValue(mainNodeId, compressEdge(additionId, edgeType));
    }
  }

  public void removeEdge(int mainNodeId, int additionId, GraphEdgeType edgeType) {
    if (edgeType.isNormalEdge()) {
      myEdges.remove(mainNodeId, compressEdge(additionId, edgeType));
      myEdges.remove(additionId, compressEdge(mainNodeId, edgeType));
    }
    else {
      myEdges.remove(mainNodeId, compressEdge(additionId, edgeType));
    }
  }

  public List<Pair<Integer, GraphEdgeType>> getEdges(int nodeId) {
    return ContainerUtil.map(myEdges.get(nodeId),
                             compressEdge -> Pair.create(convertToInteger(retrievedNodeId(compressEdge)), retrievedType(compressEdge)));
  }

  public int[] getKnownIds() {
    return myEdges.keys();
  }

  private static @Nullable Integer convertToInteger(int value) {
    return value == NULL_ID ? null : value;
  }

  private static int compressEdge(int nodeId, GraphEdgeType edgeType) {
    assert nodeId == NULL_ID || (nodeId < MAX_NODE_ID && nodeId > MIN_NODE_ID);
    int type = edgeType.ordinal();
    return (type << EDGE_BITS_OFFSET) | (COMPRESSED_NODE_ID_MASK & nodeId);
  }

  private static @NotNull GraphEdgeType retrievedType(int compressEdge) {
    int type = compressEdge >>> EDGE_BITS_OFFSET;
    return GraphEdgeType.values()[type];
  }

  private static int retrievedNodeId(int compressEdge) {
    return (compressEdge << EDGE_TYPE_BITS) >> EDGE_TYPE_BITS;
  }

  public void removeAll() {
    myEdges.clear();
  }
}
