package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}
