package com.babyduck.vm;

import com.babyduck.intermediate.ConstantTable;
import com.babyduck.intermediate.Segment;
import com.babyduck.intermediate.VirtualMemory;
import com.babyduck.semantic.MemorySize;
import com.babyduck.semantic.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la memoria de ejecución: traducción de direcciones, constantes cargadas,
 * casillas sin valor y un registro de activación por llamada
 */
class ExecutionMemoryTest {

    private final ConstantTable table = new ConstantTable(new VirtualMemory());

    /** Memoria con 2 globales int, 1 global float y 1 temporal int en main */
    private ExecutionMemory memory() {
        return new ExecutionMemory(new MemorySize(2, 1, 0), new MemorySize(1, 0, 0), table);
    }

    @Test
    void laDireccionDaSegmentoTipoYCasilla() {
        assertEquals(new VirtualMemory.Range(Segment.TEMPORARY, Type.BOOL), VirtualMemory.rangeOf(7003));
        assertEquals(new VirtualMemory.Range(Segment.CONSTANT, Type.STRING), VirtualMemory.rangeOf(10000));
        assertEquals(3, VirtualMemory.cellOf(7003));
        assertNull(VirtualMemory.rangeOf(500));
        assertNull(VirtualMemory.rangeOf(11000));
    }

    @Test
    void lasConstantesSeCarganEnSuDireccion() {
        assertEquals(8000, table.addressOf(Type.INT, 7));
        assertEquals(9000, table.addressOf(Type.FLOAT, 2.5));
        assertEquals(10000, table.addressOf(Type.STRING, "hola"));
        ExecutionMemory memory = memory();

        assertEquals(7, memory.readInt(8000));
        assertEquals(7.0, memory.readFloat(8000));               // int se lee como float
        assertEquals(2.5, memory.readFloat(9000));
        assertEquals("hola", memory.readString(10000));
    }

    @Test
    void leerUnaCasillaSinValorEsErrorDeEjecucion() {
        ExecutionMemory memory = memory();
        memory.writeInt(1000, 4);
        assertEquals(4, memory.readInt(1000));

        RuntimeError error = assertThrows(RuntimeError.class, () -> memory.readInt(1001));
        assertEquals("la dirección 1001 no tiene valor", error.detail());
        assertEquals("Error de ejecución en el cuádruplo 5: la dirección 1001 no tiene valor",
                error.atQuad(5).getMessage());
    }

    @Test
    void cadaLlamadaTieneSusPropiasLocalesYTemporales() {
        table.addressOf(Type.INT, 7);                            // 8000
        ExecutionMemory memory = memory();
        memory.writeInt(5000, 1);                                // temporal de main

        memory.prepareCall(new MemorySize(1, 1, 0), new MemorySize(1, 0, 0));    // ERA
        memory.writeParam(3000, 5000);                           // PARAM: se lee en main
        memory.writeParam(4000, 8000);                           // PARAM float ← int
        memory.enterCall(9);                                     // GOSUB
        assertEquals(2, memory.depth());
        assertEquals(1, memory.readInt(3000));
        assertEquals(7.0, memory.readFloat(4000));
        assertThrows(RuntimeError.class, () -> memory.readInt(5000));   // 5000 de la llamada, vacío
        memory.writeInt(5000, 2);

        assertEquals(9, memory.exitCall());                      // ENDFUNC
        assertEquals(1, memory.depth());
        assertEquals(1, memory.readInt(5000));                   // el temporal de main no cambió
    }

    @Test
    void demasiadasLlamadasAnidadasEsErrorDeEjecucion() {
        ExecutionMemory memory = memory();
        RuntimeError error = assertThrows(RuntimeError.class, () -> {
            while (true) {
                memory.prepareCall(MemorySize.EMPTY, MemorySize.EMPTY);
                memory.enterCall(1);
            }
        });
        assertTrue(error.detail().contains("llamadas anidadas"));
        assertEquals(ExecutionMemory.MAX_CALLS, memory.depth());
    }
}
