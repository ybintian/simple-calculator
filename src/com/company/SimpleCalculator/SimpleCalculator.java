package com.company.SimpleCalculator;

import com.company.SimpleLexer.SimpleLexer;
import com.company.SimpleLexer.Token;
import com.company.SimpleLexer.TokenReader;
import com.company.SimpleLexer.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SimpleCalculator {
    private HashMap<String, Integer> variables = new HashMap<String, Integer>();
    private static boolean verbose = false;

    public static void main(String[] args) {
        SimpleCalculator calculator = new SimpleCalculator();
        String script = "int a = b+3;";
        System.out.println("解析变量申明语句：" + script);
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(script);

        try {
            SimpleASTNode node = calculator.intDeclare(tokens);
            calculator.dumpAST(node, "");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        //测试表达式
        script = "2+3*5";
        System.out.println("\n计算: " + script + "，看上去一切正常。");
        calculator.evaluate(script);

        //测试语法错误
        script = "2+";
        System.out.println("\n: " + script + "，应该有语法错误。");
        calculator.evaluate(script);

        script = "2+3+4";
        System.out.println("\n计算: " + script + "，结合性出现错误。");
        calculator.evaluate(script);
    }

    public void evaluate(String script) {
        try {
            ASTNode tree = parse(script);
            dumpAST(tree, "");
            evaluate(tree, "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public ASTNode parse(String code) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(code);
        ASTNode rootNode = prog(tokens);

        return rootNode;
    }

    private int evaluate(ASTNode node, String indent) throws Exception {
        Integer result = null;
        if (verbose) {
            System.out.print(indent + "Calculator: " + node.getType());
        }

        switch(node.getType()) {
        case Programm:
            for(ASTNode child : node.getChildren()) {
                result = evaluate(child, indent + "\t");
            }
            break;
        case Additive:
            ASTNode child1 = node.getChildren().get(0);
            int value1 = evaluate(child1, indent + "\t");
            ASTNode child2 = node.getChildren().get(1);
            int value2 = evaluate(child2, indent + "\t");
            if(node.getText().equals("+")) {
                result = value1 + value2;
            } else {
                result = value1 - value2;
            }
            break;
        case Multiplicative:
            child1 = node.getChildren().get(0);
            value1 = evaluate(child1, indent + "\t");
            child2 = node.getChildren().get(1);
            value2 = evaluate(child2, indent + "\t");
            if(node.getText().equals("*")) {
                result = value1 * value2;
            } else {
                result = value1 / value2;
            }
            break;
        case IntLiteral:
            result = Integer.valueOf(node.getText()).intValue();
            break;
        case Identifier:
            String varName = node.getText();
            if(variables.containsKey(varName)) {
                Integer value = variables.get(varName);
                if (value != null) {
                    result = value.intValue();
                } else {
                    throw new Exception("unknown variable:" + varName);
                }
            }
            break;
        case AssignmentStmt:
            varName = node.getText();
            if (!variables.containsKey(varName)) {
                throw new Exception("unknown variable:" + varName);
            }
        case IntDeclaration:
            varName = node.getText();
            Integer varValue = null;
            if (node.getChildren().size() > 0) {
                ASTNode child = node.getChildren().get(0);
                result = evaluate(child, indent + "\t");
                varValue = Integer.valueOf(result);
            }
            variables.put(varName, varValue);
            break;
        default:
        }

        if (verbose) {
            System.out.println(indent + "Result:" + result);
        } else if(indent.equals("")) {
            if (node.getType() == ASTNodeType.IntDeclaration || node.getType() == ASTNodeType.AssignmentStmt) {
                System.out.println(node.getText() + ":" + result);
            } else if (node.getType() != ASTNodeType.Programm) {
                System.out.println(result);
            }
        }
        return result;
    }

    private SimpleASTNode prog(TokenReader tokens) throws Exception {
        SimpleASTNode node = new SimpleASTNode(ASTNodeType.Programm, "Calculator");
        SimpleASTNode child = additive(tokens);

        if (child != null) {
            node.addChild(child);
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

    private void dumpAST(ASTNode node, String indent) {
        System.out.println(indent + node.getType() + " " + node.getText());
        for (ASTNode child : node.getChildren()) {
            dumpAST(child, indent + "\t");
        }
    }
}
