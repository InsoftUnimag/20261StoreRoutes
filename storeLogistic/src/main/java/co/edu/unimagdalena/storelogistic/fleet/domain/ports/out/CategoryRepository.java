package co.edu.unimagdalena.storelogistic.fleet.domain.ports.out;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Category;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(Long categoryId);
    Optional<Category> findByType(CategoryType type);
}