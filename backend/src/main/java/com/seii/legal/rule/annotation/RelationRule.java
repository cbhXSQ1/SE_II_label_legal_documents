package com.seii.legal.rule.annotation;

import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.Proposition;
import com.seii.legal.data.entity.Relation;

import java.util.List;

public interface RelationRule {
    String classify(Proposition a, Proposition b, String guideVersion);

    String nextStep(List<Relation> existing);

    List<String> sortComposite(List<String> propositionIds);

    String matchDirection(Proposition individual, Proposition general);

    String attackDirection(Proposition attacker, Proposition target);

    boolean isSame(Proposition a, Proposition b);

    boolean isValidNesting(String expression);

    List<String> findIsolated(List<Relation> relations, List<String> propositionIds);

    Agreement compareRelations(List<Relation> a, List<Relation> b);
}
