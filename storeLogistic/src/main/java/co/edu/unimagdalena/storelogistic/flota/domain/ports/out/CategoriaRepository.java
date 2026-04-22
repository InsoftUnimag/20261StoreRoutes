package co.edu.unimagdalena.storelogistic.flota.domain.ports.out;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Categoria;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import java.util.Optional;

public interface CategoriaRepository {
    Optional<Categoria> findById(Long idCategoria);
    Optional<Categoria> findByTipo(TipoCategoria tipo);
}

