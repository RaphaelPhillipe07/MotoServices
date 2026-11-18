package br.com.motoservices.dao;

import br.com.motoservices.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioDAO extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
