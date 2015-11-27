package com.kiroule.jpetstore.vaadinspring.ui.view;

import static java.lang.String.format;

import com.kiroule.jpetstore.vaadinspring.domain.Item;
import com.kiroule.jpetstore.vaadinspring.domain.Product;
import com.kiroule.jpetstore.vaadinspring.persistence.ItemMapper;
import com.kiroule.jpetstore.vaadinspring.persistence.ProductMapper;
import com.kiroule.jpetstore.vaadinspring.ui.form.ItemForm;
import com.kiroule.jpetstore.vaadinspring.ui.util.ViewConfig;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Window;

import org.vaadin.viritin.fields.MTable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author Igor Baiborodine
 */
@SpringView(name = ItemListView.VIEW_NAME)
@ViewConfig(displayName = "Item")
public class ItemListView extends AbstractView {

  public static final String VIEW_NAME = "item-list";

  @Resource
  private ProductMapper productRepository;

  @Resource
  private ItemMapper itemRepository;

  private MTable<Item> itemList;
  private Product product;

  @PostConstruct
  public void init() {

    itemList = new MTable<>(Item.class)
        .withProperties("itemId", "product", "listPrice", "attribute5")
        .withColumnHeaders("Item ID", "Description", "List Price", "")
        .setSortableProperties("itemId", "product", "listPrice")
        .withGeneratedColumn("itemId", entity -> {
          Button itemIdButton = new Button(entity.getItemId(), this::viewDetails);
          itemIdButton.setData(entity);
          itemIdButton.addStyleName("link");
          return itemIdButton;
        })
        .withGeneratedColumn("product", entity -> entity.getAttribute1() + " " + product.getName())
        .withGeneratedColumn("attribute5", entity -> {
          //String uri = ShoppingCartView.VIEW_NAME + "/" + entity.getItemId;
          Button addToCartButton = new Button("Add to Cart",
              // TODO: implement me
              //click -> UIEventBus.post(new UINavigationEvent(uri)));
              click -> Notification.show(format("Adding %s item to shopping cart", click.getButton().getData()),
                  Notification.Type.HUMANIZED_MESSAGE));
          addToCartButton.setData(entity.getItemId());
          return addToCartButton;
        })
        .withFullWidth();

    addComponents(getTitle(), itemList);
    setSizeFull();
    expand(itemList);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    product = productRepository.getProduct(event.getParameters());
    title.setValue(format("%s | %s", product.getName(), product.getProductId()));
    itemList.setBeans(itemRepository.getItemListByProduct(product.getProductId()));
  }

  private void viewDetails(Button.ClickEvent click) {
    ItemForm itemForm = new ItemForm((Item) click.getButton().getData());
    Window popup = itemForm.openInModalPopup();
    popup.setCaption("View Details");
  }
}