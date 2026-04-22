package co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Categoria;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.CategoriaRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.mapper.CategoriaMapper;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jparepository.CategoriaSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoriaRepositoryAdapter implements CategoriaRepository {

    private final CategoriaSpringRepository springRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    public Optional<Categoria> findById(Long idCategoria) {
        return springRepository.findById(idCategoria)
                .map(categoriaMapper::toDomain);
    }

    @Override
    public Optional<Categoria> findByTipo(TipoCategoria tipo) {
        return springRepository.findByTipo(tipo.name())
                .map(categoriaMapper::toDomain);
    }
}