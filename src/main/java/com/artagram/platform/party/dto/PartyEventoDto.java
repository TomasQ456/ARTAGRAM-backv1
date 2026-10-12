package com.artagram.platform.party.dto;

/**
 * Todo lo que viaja por /topic/fiesta/{salaId}: el reloj (TICK), el cierre de
 * ronda, la aparicion/toma/liberacion del borrador y los trazos nuevos.
 */
public record PartyEventoDto(
        String tipo,
        Long msRestantes,
        PartyTrazoDto trazo,
        String propietarioBorrador
) {
    public static PartyEventoDto tick(long msRestantes) {
        return new PartyEventoDto("TICK", msRestantes, null, null);
    }

    public static PartyEventoDto rondaTerminada() {
        return new PartyEventoDto("RONDA_TERMINADA", 0L, null, null);
    }

    public static PartyEventoDto borradorDisponible() {
        return new PartyEventoDto("BORRADOR_DISPONIBLE", null, null, null);
    }

    public static PartyEventoDto borradorTomado(String propietario) {
        return new PartyEventoDto("BORRADOR_TOMADO", null, null, propietario);
    }

    public static PartyEventoDto borradorLiberado() {
        return new PartyEventoDto("BORRADOR_LIBERADO", null, null, null);
    }

    public static PartyEventoDto trazoNuevo(PartyTrazoDto trazo) {
        return new PartyEventoDto("TRAZO", null, trazo, null);
    }
}
