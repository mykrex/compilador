package com.babyduck.semantic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VariableTableTest {

    @Test
    void addAndGet() {
        VariableTable table = new VariableTable();
        assertTrue(table.add(new VariableInfo("_A_", Type.INT)));
        assertTrue(table.contains("_A_"));
        assertEquals(Type.INT, table.get("_A_").type());
    }

    /** El duplicado se rechaza con false y se conserva la primera declaración */
    @Test
    void duplicateIsRejectedAndFirstIsKept() {
        VariableTable table = new VariableTable();
        table.add(new VariableInfo("_A_", Type.INT));
        assertFalse(table.add(new VariableInfo("_A_", Type.FLOAT)));
        assertEquals(Type.INT, table.get("_A_").type());
        assertEquals(1, table.size());
    }

    @Test
    void missingVariableReturnsNull() {
        VariableTable table = new VariableTable();
        assertNull(table.get("_X_"));
        assertFalse(table.contains("_X_"));
    }

    /** LinkedHashMap conserva el orden de declaración al recorrer */
    @Test
    void keepsDeclarationOrder() {
        VariableTable table = new VariableTable();
        table.add(new VariableInfo("_Z_", Type.INT));
        table.add(new VariableInfo("_A_", Type.FLOAT));
        table.add(new VariableInfo("_M_", Type.INT));
        List<String> names = table.all().stream().map(VariableInfo::name).toList();
        assertEquals(List.of("_Z_", "_A_", "_M_"), names);
    }
}