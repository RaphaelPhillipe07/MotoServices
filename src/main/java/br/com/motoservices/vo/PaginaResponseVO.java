package br.com.motoservices.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação da API. Mantém o contrato estável mesmo quando
 * a listagem é servida diretamente de uma consulta paginada ou de um item único.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResponseVO<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <E, R> PaginaResponseVO<R> de(Page<E> pagina, Function<E, R> mapper) {
        return new PaginaResponseVO<>(
                pagina.getContent().stream().map(mapper).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }

    public static <T> PaginaResponseVO<T> de(List<T> itens, int page, int size) {
        return new PaginaResponseVO<>(itens, page, size, itens.size(), itens.isEmpty() ? 0 : 1, true, true);
    }
}
