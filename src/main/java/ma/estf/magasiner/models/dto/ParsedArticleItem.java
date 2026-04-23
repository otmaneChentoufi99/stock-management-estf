package ma.estf.magasiner.models.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsedArticleItem {
    private String designation;
    private int quantity;
    private BooleanProperty needsInventoryNumber;

    public ParsedArticleItem(String designation, int quantity, boolean needsInventoryNumber) {
        this.designation = designation;
        this.quantity = quantity;
        this.needsInventoryNumber = new SimpleBooleanProperty(needsInventoryNumber);
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
}
