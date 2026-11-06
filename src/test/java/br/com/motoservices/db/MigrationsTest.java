package br.com.motoservices.db;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aplica as migrações Flyway em um PostgreSQL real (embarcado, sem Docker)
 * e confere índices, constraints e a extensão de busca por trigram.
 */
class MigrationsTest {

    private static final Set<String> INDICES_ESPERADOS = Set.of(
            "idx_os_cliente",
            "idx_os_moto",
            "idx_os_status",
            "idx_os_data_abertura",
            "idx_servico_os",
            "idx_troca_peca_peca",
            "idx_moto_cilindradas",
            "uk_moto_marca_modelo",
            "idx_cliente_nome_trgm"
    );

    private static final Set<String> CHECKS_ESPERADOS = Set.of(
            "ck_peca_valor_nao_negativo",
            "ck_peca_estoque_nao_negativo",
            "ck_peca_estoque_minimo_nao_negativo",
            "ck_servico_valor_base_nao_negativo",
            "ck_os_status_valido"
    );

    @Test
    @DisplayName("V1 + V2 aplicam em PostgreSQL real com índices, checks e pg_trgm")
    void aplicaMigracoesEmPostgresReal() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            DataSource dataSource = postgres.getPostgresDatabase();

            MigrateResult resultado = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .schemas("motoservices")
                    .defaultSchema("motoservices")
                    .load()
                    .migrate();

            assertEquals(5, resultado.migrationsExecuted, "V1 a V5 devem ser aplicadas");

            try (Connection conexao = dataSource.getConnection()) {
                assertTrue(consultar(conexao,
                                "SELECT indexname FROM pg_indexes WHERE schemaname = 'motoservices'")
                                .containsAll(INDICES_ESPERADOS),
                        "todos os índices da V2 devem existir");

                assertTrue(consultar(conexao,
                                "SELECT constraint_name FROM information_schema.table_constraints "
                                        + "WHERE constraint_schema = 'motoservices' AND constraint_type = 'CHECK'")
                                .containsAll(CHECKS_ESPERADOS),
                        "todos os CHECKs da V2 devem existir");

                assertTrue(consultar(conexao,
                                "SELECT extname FROM pg_extension WHERE extname = 'pg_trgm'")
                                .contains("pg_trgm"),
                        "pg_trgm deve estar instalada");

                Set<String> tabelasComVersao = consultar(conexao,
                        "SELECT table_name FROM information_schema.columns "
                                + "WHERE table_schema = 'motoservices' AND column_name = 'versao'");
                assertTrue(tabelasComVersao.containsAll(Set.of("tb_peca", "tb_ordem_de_servico")),
                        "V3 deve adicionar a coluna versao nas tabelas de peça e OS");

                Set<String> colunasOs = consultar(conexao,
                        "SELECT column_name FROM information_schema.columns "
                                + "WHERE table_schema = 'motoservices' AND table_name = 'tb_ordem_de_servico'");
                assertTrue(colunasOs.containsAll(Set.of(
                                "total_pecas", "total_mao_de_obra", "desconto_aplicado", "total_final")),
                        "V4 deve adicionar as colunas de snapshot do orçamento");

                assertTrue(consultar(conexao,
                                "SELECT table_name FROM information_schema.tables "
                                        + "WHERE table_schema = 'motoservices'")
                                .contains("tb_os_status_historico"),
                        "V5 deve criar a tabela de histórico de status");

                assertThrows(SQLException.class, () -> executar(conexao,
                                "INSERT INTO motoservices.tb_peca "
                                        + "(codigo, descricao, valor, quantidade_estoque, estoque_minimo) "
                                        + "VALUES ('X', 'Peça inválida', -1, 0, 0)"),
                        "CHECK de valor não negativo deve rejeitar valor negativo");

                executar(conexao, "INSERT INTO motoservices.tb_moto (marca, modelo, cilindradas) "
                        + "VALUES ('Honda', 'CG 160', 160)");
                assertThrows(SQLException.class, () -> executar(conexao,
                                "INSERT INTO motoservices.tb_moto (marca, modelo, cilindradas) "
                                        + "VALUES ('honda', 'cg 160', 160)"),
                        "índice único case-insensitive deve impedir duplicata");
            }
        }
    }

    private static Set<String> consultar(Connection conexao, String sql) throws SQLException {
        Set<String> valores = new HashSet<>();
        try (Statement statement = conexao.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            while (resultado.next()) {
                valores.add(resultado.getString(1));
            }
        }
        return valores;
    }

    private static void executar(Connection conexao, String sql) throws SQLException {
        try (Statement statement = conexao.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
