package co.edu.unimagdalena.storelogistic.fleet.domain.models;

import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    private Long categoryId;
    private CategoryType type;
    private LoadCapacity maxCapacity;
}