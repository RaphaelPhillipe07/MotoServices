package br.com.motoservices.controller;

import br.com.motoservices.bo.ClienteBO;
import br.com.motoservices.vo.ClienteVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro, busca e histórico de revisões")
public class ClienteController {

    private final ClienteBO clienteBO;

    @PostMapping
    public ResponseEntity<ClienteVO> cadastrar(@RequestBody ClienteVO clienteVO) {
        return ResponseEntity.ok(clienteBO.cadastrar(clienteVO));
    }

    @GetMapping
    public ResponseEntity<List<ClienteVO>> buscar(@RequestParam String nomeOuCpf) {
        return ResponseEntity.ok(clienteBO.buscar(nomeOuCpf));
    }
}
