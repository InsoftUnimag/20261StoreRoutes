package co.edu.unimagdalena.storelogistic.domain.fleet.ports.out;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Category;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.CategoryType;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(Long categoryId);
    Optional<Category> findByType(CategoryType type);
}