package com.kiroule.jpetstore.vaadinspring.ui.view;

import com.kiroule.jpetstore.vaadinspring.domain.BillingDetails;
import com.kiroule.jpetstore.vaadinspring.domain.Cart;
import com.kiroule.jpetstore.vaadinspring.domain.Order;
import com.kiroule.jpetstore.vaadinspring.domain.OrderDetails;
import com.kiroule.jpetstore.vaadinspring.domain.ShippingDetails;
import com.kiroule.jpetstore.vaadinspring.service.OrderService;
import com.kiroule.jpetstore.vaadinspring.ui.component.CartItemListTable;
import com.kiroule.jpetstore.vaadinspring.ui.converter.CurrencyConverter;
import com.kiroule.jpetstore.vaadinspring.ui.event.UIEventBus;
import com.kiroule.jpetstore.vaadinspring.ui.event.UINavigationEvent;
import com.kiroule.jpetstore.vaadinspring.ui.theme.JPetStoreTheme;
import com.kiroule.jpetstore.vaadinspring.ui.util.CurrentAccount;
import com.kiroule.jpetstore.vaadinspring.ui.util.CurrentCart;
import com.kiroule.jpetstore.vaadinspring.ui.util.ViewConfig;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MFormLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.math.BigDecimal;

import javax.annotation.PostConstruct;

import static com.kiroule.jpetstore.vaadinspring.ui.util.CurrentCart.Key;
import static com.kiroule.jpetstore.vaadinspring.ui.util.CurrentCart.Key.BILLING_DETAILS;
import static com.kiroule.jpetstore.vaadinspring.ui.util.CurrentCart.Key.SHIPPING_DETAILS;
import static com.kiroule.jpetstore.vaadinspring.ui.util.CurrentCart.Key.SHOPPING_CART;
import static java.lang.String.format;

/**
 * @author Igor Baiborodine
 */
@SpringView(name = ConfirmOrderView.VIEW_NAME)
@ViewConfig(displayName = "Confirm Order", authRequired = true)
public class ConfirmOrderView extends AbstractView {

  private static final long serialVersionUID = 2011890008388273461L;

  public static final String VIEW_NAME = "confirm-order";
  public static final String SUBTOTAL_LABEL_PATTERN = "Subtotal: %s";

  @Autowired
  private OrderService orderService;
  @Autowired
  private CartItemListTable cartItemList;

  private MHorizontalLayout orderDetailsLayout = new MHorizontalLayout().withMargin(false);
  private PaymentMethodFormLayout paymentMethodFormLayout = new PaymentMethodFormLayout();
  private OrderDetailsFormLayout billingDetailsFormLayout = new OrderDetailsFormLayout();
  private OrderDetailsFormLayout shippingDetailsFormLayout = new OrderDetailsFormLayout();
  private MButton placeOrderButton = createPlaceOrderButton();
  private MButton viewOrdersButton = createViewOrdersButton();
  private Label subtotalLabel = createSubtotalLabel();

  @PostConstruct
  void init() {

    Panel paymentMethodPanel = new Panel("Payment Method", paymentMethodFormLayout);
    Panel billingDetailsPanel = new Panel("Billing Details", billingDetailsFormLayout);
    Panel shippingDetailsPanel = new Panel("Shipping Details", shippingDetailsFormLayout);
    orderDetailsLayout.addComponents(paymentMethodPanel, billingDetailsPanel, shippingDetailsPanel);
    orderDetailsLayout.setExpandRatio(paymentMethodPanel, 1.0f);
    orderDetailsLayout.setExpandRatio(billingDetailsPanel, 1.0f);
    orderDetailsLayout.setExpandRatio(shippingDetailsPanel, 1.0f);
    orderDetailsLayout.setSizeFull();
    viewOrdersButton.setVisible(false);
    int actualRowCount = cartItemList.getContainerDataSource().size();
    cartItemList.setPageLength(Math.min(actualRowCount, 5));

    MVerticalLayout content = new MVerticalLayout(cartItemList, subtotalLabel, orderDetailsLayout);
    Panel contentPanel = new Panel(content);
    addComponents(initTitleLabel(), contentPanel,
        placeOrderButton, viewOrdersButton);
    setSizeFull();
    expand(contentPanel);
  }

  @Override
  public void executeOnEnter(ViewChangeListener.ViewChangeEvent event) {

    if (CurrentCart.isEmpty()
        || CurrentCart.get(BILLING_DETAILS) == null
        || CurrentCart.get(SHIPPING_DETAILS) == null) {
      UIEventBus.post(new UINavigationEvent(CartView.VIEW_NAME));
      return;
    }
    Cart cart = (Cart) CurrentCart.get(SHOPPING_CART);
    cartItemList.setBeans(cart.getCartItemList());
    subtotalLabel.setValue(format(SUBTOTAL_LABEL_PATTERN, formatSubtotal(cart.getSubTotal())));

    paymentMethodFormLayout.setEntity((BillingDetails) CurrentCart.get(BILLING_DETAILS));
    billingDetailsFormLayout.setEntity((OrderDetails) CurrentCart.get(BILLING_DETAILS));
    shippingDetailsFormLayout.setEntity((OrderDetails) CurrentCart.get(SHIPPING_DETAILS));
  }

  private Label createSubtotalLabel() {
    Label label = new Label();
    label.addStyleName(JPetStoreTheme.VIEW_LABEL_MEDIUM);
    return label;
  }

  private MButton createPlaceOrderButton() {

    return new MButton("Place Your Order").withListener(event -> {

      Order order = new Order();
      order.initOrder(CurrentAccount.get().getUsername(), (BillingDetails) CurrentCart.get(BILLING_DETAILS),
          (ShippingDetails) CurrentCart.get(SHIPPING_DETAILS), (Cart) CurrentCart.get(Key.SHOPPING_CART));
      try {
        orderService.insertOrder(order);
        CurrentCart.clear();
        event.getButton().setVisible(false);
        viewOrdersButton.setVisible(true);
        cartItemList.setReadOnly(true);

        showConfirmation("Thank you, your order has been submitted.");
      } catch (Exception e) {
        showError("An error occurred while inserting a new order: " + e.getMessage());
      }
    });
  }

  private MButton createViewOrdersButton() {
    return new MButton("View My Orders")
        .withListener(event -> UIEventBus.post(new UINavigationEvent(OrderListView.VIEW_NAME)));
  }

  private String formatSubtotal(BigDecimal subtotal) {
    return new CurrencyConverter().convertToPresentation(subtotal, String.class, UI.getCurrent().getLocale());
  }
}

class PaymentMethodFormLayout extends MFormLayout {

  private static final long serialVersionUID = 468079442198856243L;

  private Label cardType = new Label();
  private Label cardNumber = new Label();
  private Label expiryDate = new Label();

  public PaymentMethodFormLayout() {

    cardType.setCaption("Card Type:");
    cardNumber.setCaption("Card Number:");
    expiryDate.setCaption("Expiry Date:");

    addComponents(cardType, cardNumber, expiryDate);
    setStyleName(JPetStoreTheme.BASE_FORM);
    withWidth("");
  }

  public void setEntity(BillingDetails billingDetails) {

    cardType.setValue(billingDetails.getCardType());
    cardNumber.setValue(billingDetails.getCardNumber());
    expiryDate.setValue(billingDetails.getExpiryDate());
  }
}

class OrderDetailsFormLayout extends MFormLayout {

  private static final long serialVersionUID = -634932385134732080L;

  private Label firstName = new Label();
  private Label lastName = new Label();
  private Label email = new Label();
  private Label phone = new Label();
  private Label address1 = new Label();
  private Label address2 = new Label();
  private Label city = new Label();
  private Label state = new Label();
  private Label zip = new Label();
  private Label country = new Label();

  public OrderDetailsFormLayout() {

    firstName.setCaption("First Name:");
    lastName.setCaption("LastName:");
    email.setCaption("Email:");
    phone.setCaption("Phone:");
    address1.setCaption("Address 1:");
    address2.setCaption("Address 2:");
    city.setCaption("City:");
    state.setCaption("State:");
    zip.setCaption("ZIP Code:");
    country.setCaption("Country:");

    addComponents(firstName, lastName, email, phone, address1, address2, city, state, zip, country);
    setStyleName(JPetStoreTheme.BASE_FORM);
    withWidth("");
  }

  public void setEntity(OrderDetails orderDetails) {

    firstName.setValue(orderDetails.getFirstName());
    lastName.setValue(orderDetails.getLastName());
    email.setValue(orderDetails.getEmail());
    phone.setValue(orderDetails.getPhone());
    address1.setValue(orderDetails.getAddress1());
    address2.setValue(orderDetails.getAddress2());
    city.setValue(orderDetails.getCity());
    state.setValue(orderDetails.getState());
    zip.setValue(orderDetails.getZip());
    country.setValue(orderDetails.getCountry());
  }
}