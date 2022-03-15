package ca.gb.comp3095.foodrecipe.model.service;

import ca.gb.comp3095.foodrecipe.controller.recipe.SearchRecipeCommand;
import ca.gb.comp3095.foodrecipe.model.domain.Recipe;
import ca.gb.comp3095.foodrecipe.model.repo.RecipeRespository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    RecipeRespository recipeRespository;

    JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();


    double threshold = 0.50;

    Predicate<Object> isNotNull = Predicate.not(Objects::isNull);

    Predicate<String> isNotBlank = StringUtils::isNotBlank;
    BiPredicate<String, String> matchAboveThreshold = (left, right) -> isNotNull.test(left) && isNotNull.test(right) && jaroWinklerSimilarity.apply(left, right) > threshold;
    BiPredicate<Recipe, SearchRecipeCommand> titleSearch = ((recipe, searchRecipeCommand) -> matchAboveThreshold.test(recipe.getTitle(), searchRecipeCommand.getTitle()));
    BiPredicate<Recipe, SearchRecipeCommand> descriptionSearch = ((recipe, searchRecipeCommand) -> matchAboveThreshold.test(recipe.getDescription(), searchRecipeCommand.getDescription()));
    BiPredicate<Recipe, SearchRecipeCommand> ingredientSearch = ((recipe, searchRecipeCommand) -> matchAboveThreshold.test(recipe.getIngredients(), searchRecipeCommand.getIngredients()));
    BiPredicate<Recipe, SearchRecipeCommand> cookingTimeUnderSearch = ((recipe, searchRecipeCommand) -> isNotNull.test(recipe.getCookingTime()) && isNotNull.test(searchRecipeCommand.getCookingTimeUnder()) && recipe.getCookingTime().toMinutes() < searchRecipeCommand.getCookingTimeUnder());

    BiPredicate<Recipe, String> titleSearchStr = ((recipe, queryString) -> matchAboveThreshold.test(recipe.getTitle(), queryString));
    BiPredicate<Recipe, String> descriptionSearchStr = ((recipe, queryString) -> matchAboveThreshold.test(recipe.getDescription(), queryString));
    BiPredicate<Recipe, String> ingredientSearchStr = ((recipe, queryString) -> matchAboveThreshold.test(recipe.getIngredients(), queryString));

    BiPredicate<Recipe, SearchRecipeCommand> searchFilters = List.of(titleSearch, descriptionSearch, ingredientSearch, cookingTimeUnderSearch).stream().reduce(BiPredicate::or).orElse((r, s) -> false);
    BiPredicate<Recipe, String> searchFilterString = List.of(titleSearchStr, descriptionSearchStr, ingredientSearchStr).stream().reduce(BiPredicate::or).orElse((r, s) -> false);

    @Override
    public List<Recipe> findAllBy(@NotNull final SearchRecipeCommand searchRecipeCommand) {
        return getQueryTerms(searchRecipeCommand)
                .stream()
                .filter(isNotNull)
                .map(this::findAllBy)
                .flatMap(Collection::stream)
                .distinct().collect(Collectors.toList());

//        return recipeRespository.findAll()
//                .stream()
//                .filter(recipe -> searchFilters.test(recipe, searchRecipeCommand))
//                .collect(Collectors.toList());

    }

    private List<String> getQueryTerms(@NotNull SearchRecipeCommand searchRecipeCommand) {
        List<String> termList = new ArrayList<>();

        if (StringUtils.isNotBlank(searchRecipeCommand.getTitle())) {
            termList.add(searchRecipeCommand.getTitle());
        }
        if (StringUtils.isNotBlank(searchRecipeCommand.getDescription())) {
            termList.add(searchRecipeCommand.getDescription());
        }
        if (StringUtils.isNotBlank(searchRecipeCommand.getIngredients())) {
            termList.add(searchRecipeCommand.getIngredients());
        }

        return termList;
    }

    @Override
    public List<Recipe> findAllBy(@NotNull final String searchQuery) {
        if (StringUtils.isBlank(searchQuery)) {
            return Collections.EMPTY_LIST;
        }
        return recipeRespository.findAll()
                .stream()
                .filter(recipe -> searchFilterString.test(recipe, searchQuery))
                .collect(Collectors.toList());

    }

    @Override
    public List<Recipe> findAll() {
        return recipeRespository.findAll();
    }
}
