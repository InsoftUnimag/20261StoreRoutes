package co.edu.unimagdalena.storelogistic.fleet.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Category;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.out.CategoryRepository;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.mapper.CategoryMapper;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.persistence.jparepository.CategorySpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategorySpringRepository springRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public Optional<Category> findById(Long categoryId) {
        return springRepository.findById(categoryId)
                .map(categoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findByType(CategoryType type) {
        return springRepository.findByType(type.name())
                .map(categoryMapper::toDomain);
    }
}