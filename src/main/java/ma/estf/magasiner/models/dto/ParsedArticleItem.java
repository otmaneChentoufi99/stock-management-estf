package ma.estf.magasiner.models.dto;

import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;
import ma.estf.magasiner.models.dto.CategoryDto;

@Getter
@Setter
public class ParsedArticleItem {
    private StringProperty designation;
    private int quantity;
    private BooleanProperty needsInventoryNumber;
    private ObjectProperty<CategoryDto> category;
    private StringProperty caracteristique;
    private DoubleProperty prixUnit;

    public ParsedArticleItem(String designation, int quantity, boolean needsInventoryNumber) {
        this.designation = new SimpleStringProperty(designation);
        this.quantity = quantity;
        this.needsInventoryNumber = new SimpleBooleanProperty(needsInventoryNumber);
        this.category = new SimpleObjectProperty<>();
        this.caracteristique = new SimpleStringProperty("");
        this.prixUnit = new SimpleDoubleProperty(0.0);
    }

    public String getDesignation() {
        return designation.get();
    }

    public void setDesignation(String value) {
        designation.set(value);
    }

    public StringProperty designationProperty() {
        return designation;
    }

    public Double getPrixUnit() {
        return prixUnit.get();
    }

    public void setPrixUnit(Double value) {
        prixUnit.set(value);
    }

    public DoubleProperty prixUnitProperty() {
        return prixUnit;
    }

    public String getCaracteristique() {
        return caracteristique.get();
    }

    public void setCaracteristique(String value) {
        caracteristique.set(value);
    }

    public StringProperty caracteristiqueProperty() {
        return caracteristique;
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
