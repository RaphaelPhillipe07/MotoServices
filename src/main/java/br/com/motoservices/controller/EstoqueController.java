package br.com.motoservices.controller;

import br.com.motoservices.bo.EstoqueBO;
import br.com.motoservices.vo.PecaVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Controle de peças e alertas de ruptura")
public class EstoqueController {

    private final EstoqueBO estoqueBO;

    @GetMapping("/baixo")
    public ResponseEntity<List<PecaVO>> listarBaixoEstoque() {
        return ResponseEntity.ok(estoqueBO.listarBaixoEstoque());
    }
}
