package br.com.motoservices.model.enums;

/**
 * Papéis de acesso ao MotoServices.
 *
 * <p>{@code ADMIN} tem acesso total; {@code ATENDENTE} opera clientes, estoque, OS e relatórios;
 * {@code MECANICO} consulta e avança o status das ordens, mas não fecha/cancela nem vê relatórios.</p>
 */
public enum Papel {
    ADMIN,
    ATENDENTE,
    MECANICO
}
