package co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Category;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.CategoryRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.CategoryType;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.mapper.CategoryMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jparepository.CategorySpringRepository;
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