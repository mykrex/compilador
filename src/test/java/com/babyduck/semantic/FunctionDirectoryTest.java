package com.babyduck.semantic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FunctionDirectoryTest {

    /** int _Suma_(_A_ : int, _B_ : float) [ var _C_ : int; { ... } ]; */
    private FunctionInfo suma() {
        FunctionInfo f = new FunctionInfo("_Suma_", Type.INT);
        f.addParam(new VariableInfo("_A_", Type.INT));
        f.addParam(new VariableInfo("_B_", Type.FLOAT));
        f.addLocal(new VariableInfo("_C_", Type.INT));
        return f;
    }

    /** Lista ordenada para llamadas y, además, entrada en la tabla local */
    @Test
    void paramsAreOrderedAndAlsoInLocals() {
        FunctionInfo f = suma();
        assertEquals(2, f.paramCount());
        assertEquals(Type.INT, f.params().get(0).type());
        assertEquals(Type.FLOAT, f.params().get(1).type());
        assertEquals(3, f.locals().size());
        // Es el mismo objeto en ambos lugares, no una copia
        assertSame(f.params().get(0), f.locals().get("_A_"));
    }

    @Test
    void duplicateParamIsRejected() {
        FunctionInfo f = new FunctionInfo("_F_", Type.VOID);
        assertTrue(f.addParam(new VariableInfo("_X_", Type.INT)));
        assertFalse(f.addParam(new VariableInfo("_X_", Type.FLOAT)));
        assertEquals(1, f.paramCount());
    }

    /** Una local que repite el nombre de un parámetro se detecta sin código extra */
    @Test
    void localWithParamNameIsRejected() {
        FunctionInfo f = suma();
        assertFalse(f.addLocal(new VariableInfo("_A_", Type.FLOAT)));
        assertEquals(2, f.paramCount());
    }

    @Test
    void duplicateFunctionIsRejected() {
        FunctionDirectory dir = new FunctionDirectory();
        assertTrue(dir.addFunction(suma()));
        assertFalse(dir.addFunction(new FunctionInfo("_Suma_", Type.VOID)));
        assertEquals(Type.INT, dir.getFunction("_Suma_").returnType());
        assertNull(dir.getFunction("_Resta_"));
    }

    /** Las globales están aparte; la búsqueda va de la función a las globales. */
    @Test
    void lookupGoesFromFunctionToGlobals() {
        FunctionDirectory dir = new FunctionDirectory();
        dir.globals().add(new VariableInfo("_G_", Type.FLOAT));
        FunctionInfo f = suma();
        dir.addFunction(f);

        assertEquals(Type.INT, dir.lookupVariable("_C_", f).type());     // local
        assertEquals(Type.FLOAT, dir.lookupVariable("_G_", f).type());   // global vista desde la función
        assertNull(dir.lookupVariable("_C_", null));                     // main solo ve globales
        assertNull(dir.lookupVariable("_X_", f));                        // no declarada
    }

    @Test
    void functionsKeepDeclarationOrder() {
        FunctionDirectory dir = new FunctionDirectory();
        dir.addFunction(new FunctionInfo("_Uno_", Type.VOID));
        dir.addFunction(new FunctionInfo("_Dos_", Type.INT));
        List<String> names = dir.functions().stream().map(FunctionInfo::name).toList();
        assertEquals(List.of("_Uno_", "_Dos_"), names);
    }
}