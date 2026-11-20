package com.babyduck.intermediate;

import com.babyduck.semantic.SemanticCube;
import com.babyduck.semantic.Type;
import com.babyduck.semantic.VariableInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la memoria virtual: rangos por segmento y tipo, reinicio por función,
 * rango lleno y tabla de constantes sin repetidos
 */
class VirtualMemoryTest {

    private final VirtualMemory memory = new VirtualMemory();

    @Test
    void basesPorSegmentoYTipo() {
        assertEquals(1000, memory.allocate(Segment.GLOBAL, Type.INT));
        assertEquals(1001, memory.allocate(Segment.GLOBAL, Type.INT));
        assertEquals(2000, memory.allocate(Segment.GLOBAL, Type.FLOAT));
        assertEquals(3000, memory.allocate(Segment.LOCAL, Type.INT));
        assertEquals(4000, memory.allocate(Segment.LOCAL, Type.FLOAT));
        assertEquals(5000, memory.allocate(Segment.TEMPORARY, Type.INT));
        assertEquals(6000, memory.allocate(Segment.TEMPORARY, Type.FLOAT));
        assertEquals(7000, memory.allocate(Segment.TEMPORARY, Type.BOOL));
        assertEquals(8000, memory.allocate(Segment.CONSTANT, Type.INT));
        assertEquals(9000, memory.allocate(Segment.CONSTANT, Type.FLOAT));
        assertEquals(10000, memory.allocate(Segment.CONSTANT, Type.STRING));
        // un segmento sin rango para ese tipo es un error del compilador
        assertThrows(IllegalArgumentException.class, () -> memory.allocate(Segment.GLOBAL, Type.BOOL));
    }

    @Test
    void cadaFuncionEmpiezaDesdeLaBaseDeLocalesYTemporales() {
        memory.allocate(Segment.GLOBAL, Type.INT);
        memory.allocate(Segment.LOCAL, Type.INT);
        memory.allocate(Segment.TEMPORARY, Type.FLOAT);
        memory.allocate(Segment.CONSTANT, Type.INT);
        assertEquals(1, memory.count(Segment.TEMPORARY, Type.FLOAT));

        memory.startScope();
        assertEquals(3000, memory.allocate(Segment.LOCAL, Type.INT));
        assertEquals(6000, memory.allocate(Segment.TEMPORARY, Type.FLOAT));
        assertEquals(1001, memory.allocate(Segment.GLOBAL, Type.INT));      // los globales siguen
        assertEquals(8001, memory.allocate(Segment.CONSTANT, Type.INT));    // las constantes siguen
    }

    @Test
    void rangoLlenoSeReportaUnaSolaVez() {
        for (int i = 0; i < VirtualMemory.SEGMENT_SIZE; i++) {
            assertEquals(5000 + i, memory.allocate(Segment.TEMPORARY, Type.INT));
        }
        assertTrue(memory.takeOverflows().isEmpty());
        assertEquals(VirtualMemory.OVERFLOW, memory.allocate(Segment.TEMPORARY, Type.INT));
        assertEquals(VirtualMemory.OVERFLOW, memory.allocate(Segment.TEMPORARY, Type.INT));
        var messages = memory.takeOverflows();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("temporales de tipo int"));
        assertTrue(memory.takeOverflows().isEmpty());                       // ya se entregó
    }

    @Test
    void constantesConElMismoValorCompartenDireccion() {
        QuadrupleGenerator gen = new QuadrupleGenerator(new SemanticCube());
        assertEquals(8000, gen.constant("2", Type.INT).address());
        assertEquals(8001, gen.constant("5", Type.INT).address());
        assertEquals(8000, gen.constant("2", Type.INT).address());
        assertEquals(9000, gen.constant("2.5", Type.FLOAT).address());
        assertEquals(9000, gen.constant("2.50", Type.FLOAT).address());     // mismo valor, otro texto
        assertEquals(9001, gen.constant("2.0", Type.FLOAT).address());      // 2.0 float no es 2 int
        assertEquals(10000, gen.string("\"hola\"").address());
        assertEquals(10000, gen.string("\"hola\"").address());
        assertEquals(2, gen.constants().size(Type.INT));
        assertEquals(2, gen.constants().size(Type.FLOAT));
        assertEquals(1, gen.constants().size(Type.STRING));
    }

    @Test
    void temporalesConNombreUnicoYDireccionReiniciada() {
        QuadrupleGenerator gen = new QuadrupleGenerator(new SemanticCube());
        Operand t1 = gen.newTemp(Type.INT);
        gen.memory().startScope();
        Operand t2 = gen.newTemp(Type.INT);
        assertEquals("t1", t1.name());
        assertEquals("t2", t2.name());
        assertEquals(5000, t1.address());
        assertEquals(5000, t2.address());
    }

    @Test
    void ningunaVariableSinDireccionLlegaAUnCuadruplo() {
        QuadrupleGenerator gen = new QuadrupleGenerator(new SemanticCube());
        assertThrows(IllegalStateException.class, () -> gen.variable(new VariableInfo("_A_", Type.INT)));
        assertEquals(1000, gen.variable(new VariableInfo("_A_", Type.INT, 1000)).address());
    }
}
