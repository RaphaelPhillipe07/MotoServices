package br.com.motoservices.controller;

import br.com.motoservices.bo.MotoBO;
import br.com.motoservices.vo.MotoVO;
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
@RequestMapping("/motos")
@RequiredArgsConstructor
@Tag(name = "Motos", description = "Catálogo dinâmico de marcas/modelos/cilindradas")
public class MotoController {

    private final MotoBO motoBO;

    @PostMapping
    public ResponseEntity<MotoVO> cadastrar(@RequestBody MotoVO motoVO) {
        return ResponseEntity.ok(motoBO.cadastrar(motoVO));
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<MotoVO>> filtrarPorCilindradas(@RequestParam int minimo,
                                                              @RequestParam int maximo) {
        return ResponseEntity.ok(motoBO.filtrarPorCilindradas(minimo, maximo));
    }
}
