package com.babyduck.semantic;

/**
 * Una variable o un parámetro declarado
 */
public record VariableInfo(String name, Type type) {

    @Override
    public String toString() {
        return name + ":" + type.display();
    }
}