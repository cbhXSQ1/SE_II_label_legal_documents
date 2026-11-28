package com.seii.legal.rule.annotation;

import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.Relation;
import java.util.List;

public interface DiagramRule {
    Diagram generate(List<Relation> relations);

    String symbol(String relationType);

    DiagramLayout layout(List<Relation> relations);

    List<String> checkConsistency(Diagram diagram, List<Relation> relations);

    Agreement compareDiagrams(Diagram a, Diagram b);

    record DiagramLayout(List<DiagramNode> nodes, List<DiagramEdge> edges) {
    }

    record DiagramNode(String nodeId, String type, int x, int y) {
    }

    record DiagramEdge(String from, String to, String symbol, String direction) {
    }
}
