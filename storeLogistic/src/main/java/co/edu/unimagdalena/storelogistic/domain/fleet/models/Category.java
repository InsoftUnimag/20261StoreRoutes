package co.edu.unimagdalena.storelogistic.domain.fleet.models;

import co.edu.unimagdalena.storelogistic.domain.fleet.values.CategoryType;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
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