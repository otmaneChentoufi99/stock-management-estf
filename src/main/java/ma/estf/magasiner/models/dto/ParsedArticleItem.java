package ma.estf.magasiner.models.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import ma.estf.magasiner.models.dto.CategoryDto;

@Getter
@Setter
public class ParsedArticleItem {
    private String designation;
    private int quantity;
    private BooleanProperty needsInventoryNumber;
    private ObjectProperty<CategoryDto> category;

    public ParsedArticleItem(String designation, int quantity, boolean needsInventoryNumber) {
        this.designation = designation;
        this.quantity = quantity;
        this.needsInventoryNumber = new SimpleBooleanProperty(needsInventoryNumber);
        this.category = new SimpleObjectProperty<>();
    }

    public boolean isNeedsInventoryNumber() {
        return needsInventoryNumber.get();
    }

    public void setNeedsInventoryNumber(boolean value) {
        needsInventoryNumber.set(value);
    }

    public BooleanProperty needsInventoryNumberProperty() {
        return needsInventoryNumber;
    }

    public CategoryDto getCategory() {
        return category.get();
    }

    public void setCategory(CategoryDto value) {
        category.set(value);
    }

    public ObjectProperty<CategoryDto> categoryProperty() {
        return category;
    }
}
