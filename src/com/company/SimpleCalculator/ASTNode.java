package com.company.SimpleCalculator;

import java.util.List;

public interface ASTNode {
    public ASTNode getParent();

    public List<ASTNode> getChildren();

    public ASTNodeType getType();

    public String getText();
}
