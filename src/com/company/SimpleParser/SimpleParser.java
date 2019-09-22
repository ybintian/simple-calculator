package com.company.SimpleParser;

import com.company.SimpleCalculator.ASTNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.company.SimpleLexer.*;
import com.company.SimpleCalculator.*;

/**
 * programm -> intDeclare | expressionStatement | assignmentStatement
 * intDeclare -> 'int' Id ( = expressionStatement ) ';'
 * expressionStatement -> additive ';'
 * addtive -> multiplicative ((+ | -) multiplicative)
 * multiplicative -> primary((* | /) primary)*
 * primary -> IntLiteral | Id | (active)
 */

public class SimpleParser {
    public static void main(String[] args) {
        System.out.println("hello my simple parser");
        SimpleParser parser = new SimpleParser();
        String script = null;
        ASTNode tree = null;

        try {
            script = "int age = 45 + 2; age = 20; age + 10 * 2;";
            System.out.println("解析：" + script);
            tree = parser.parse(script);
            parser.dumpAST(tree, "");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public ASTNode parse(String script) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(script);
        ASTNode rootNode = prog(tokens);
        return rootNode;
    }

    private SimpleASTNode prog(TokenReader tokens) throws Exception {
        SimpleASTNode node = new SimpleASTNode(ASTNodeType.Programm, "pwc");

        while(tokens.peek() != null) {
            SimpleASTNode child = intDeclare(tokens);

            if (child == null) {
                child = expressionStatement(tokens);
            }

            if (child == null) {
                child = assignmentStatement(tokens);
            }

            if (child != null) {
                node.addChild(child);
            } else {
                throw new Exception("unknown statement");
            }
        }

        return node;
    }

    private SimpleASTNode intDeclare(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if( token != null && token.getType() == TokenType.Int) {
            token = tokens.read();
            if(tokens.peek().getType() == TokenType.Identifier) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.IntDeclaration, token.getText());
                token = tokens.peek();
                if(token != null && token.getType() == TokenType.Assignment) {
                    tokens.read();
                    SimpleASTNode child = additive(tokens);
                    if(child == null) {
                        throw new Exception("invalid variable initialization expecting an expression");
                    } else {
                        node.addChild(child);
                    }
                }
            } else {
                throw new Exception("variable name expected");
            }

            if (node != null) {
                token = tokens.peek();
                if (token != null && token.getType() == TokenType.SemiColon) {
                    tokens.read();
                } else {
                    throw new Exception("invalid statement, expecting semicolon");
                }
            }
        }

        return node;
    }

    private SimpleASTNode assignmentStatement(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if (token != null && token.getType() == TokenType.Identifier) {
            token = tokens.read();
            node = new SimpleASTNode(ASTNodeType.AssignmentStmt, token.getText());
            token = tokens.peek();
            if (token != null && token.getType() == TokenType.Assignment) {
                tokens.read();
                SimpleASTNode child = additive(tokens);
                if (child == null) {
                    throw new Exception("invalid assignment statement, expecting an expression");
                } else {
                    node.addChild(child);
                    token = tokens.peek();
                    if(token != null && token.getType() == TokenType.SemiColon) {
                        tokens.read();
                    } else {
                        throw new Exception("invalid statement, expecting semicolon");
                    }
                }
            } else {
                tokens.unread();
                node = null;
            }
        } else {
            tokens.unread();
            node = null;
        }

        return node;
    }

    private SimpleASTNode expressionStatement(TokenReader tokens) throws Exception {
        int pos = tokens.getPosition();
        SimpleASTNode node = additive(tokens);
        if (node != null) {
            Token token = tokens.peek();
            if(token != null && token.getType() == TokenType.SemiColon) {
                tokens.read();
            } else {
                node = null;
                tokens.setPosition(pos);
            }
        }
        return node;
    }

    private SimpleASTNode additive(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = multiplicative(tokens);
        SimpleASTNode node = child1;
        if (child1 != null) {
            while(true) {
                Token token = tokens.peek();
                if (token != null && (token.getType() == TokenType.Plus || token.getType() == TokenType.Minus)) {
                    token = tokens.read();
                    SimpleASTNode child2 = multiplicative(tokens);
                    node = new SimpleASTNode(ASTNodeType.Additive, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                    child1 = node;
                } else {
                    break;
                }
            }
        }
        return node;
    }

    private SimpleASTNode multiplicative(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = primary(tokens);
        SimpleASTNode node = child1;

        Token token = tokens.peek();
        if(child1 != null && token != null) {
            if (token.getType() == TokenType.Star || token.getType() == TokenType.Slash) {
                token = tokens.read();
                SimpleASTNode child2 = primary(tokens);
                if (child2 != null) {
                    node = new SimpleASTNode(ASTNodeType.Multiplicative, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                } else {
                    throw new Exception("invalid multiplicative expression, expecting the right part.");
                }
            }
        }
        return node;
    }

    private SimpleASTNode primary(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if(token != null) {
            if (token.getType() == TokenType.IntLiteral) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.IntLiteral, token.getText());
            } else if(token.getType() == TokenType.Identifier) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.Identifier, token.getText());
            } else if(token.getType() == TokenType.LeftParen) {
                tokens.read();
                node = additive(tokens);
                if (node != null) {
                    token = tokens.peek();
                    if(token != null && token.getType() == TokenType.RightParen) {
                        tokens.read();
                    } else {
                        throw new Exception("expection right parenthesis");
                    }
                } else {
                    throw new Exception("expecting an additive expression inside parenthesis");
                }
            }
        }

        return node;
    }

    private class SimpleASTNode implements ASTNode {
        SimpleASTNode parent = null;
        List<ASTNode> children = new ArrayList<ASTNode>();
        List<ASTNode> readonlyChildren = Collections.unmodifiableList(children);
        ASTNodeType nodeType = null;
        String text = null;

        public SimpleASTNode(ASTNodeType nodeType, String text) {
            this.nodeType = nodeType;
            this.text = text;
        }

        @Override
        public ASTNode getParent() {
            return parent;
        }

        @Override
        public List<ASTNode> getChildren() {
            return readonlyChildren;
        }

        @Override
        public ASTNodeType getType() {
            return nodeType;
        }

        @Override
        public String getText() {
            return text;
        }

        public void addChild(SimpleASTNode child) {
            children.add(child);
            child.parent = this;
        }
    }

    public void dumpAST(ASTNode node, String indent) {
        System.out.println(indent + node.getType() + " " + node.getText());
        for (ASTNode child : node.getChildren()) {
            dumpAST(child, indent + "\t");
        }
    }
}
