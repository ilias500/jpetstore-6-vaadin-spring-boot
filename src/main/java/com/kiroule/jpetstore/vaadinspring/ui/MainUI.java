package com.kiroule.jpetstore.vaadinspring.ui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import com.kiroule.jpetstore.vaadinspring.ui.event.UILoginEvent;
import com.kiroule.jpetstore.vaadinspring.ui.event.UILogoutEvent;
import com.kiroule.jpetstore.vaadinspring.ui.event.UINavigationEvent;
import com.kiroule.jpetstore.vaadinspring.ui.menu.LeftNavBar;
import com.kiroule.jpetstore.vaadinspring.ui.menu.TopNavBar;
import com.kiroule.jpetstore.vaadinspring.ui.util.CurrentAccount;
import com.kiroule.jpetstore.vaadinspring.ui.util.NavBarButtonUpdater;
import com.kiroule.jpetstore.vaadinspring.ui.util.PageTitleUpdater;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Igor Baiborodine
 */
@Title("JPetStore 6 Demo Spring Vaadin")
@Theme("jpetstoretheme")
@Widgetset("JPetStore6Widgetset")
@SpringUI
public class MainUI extends UI {

  private final static Logger logger = LoggerFactory.getLogger(MainUI.class);

  @Autowired
  private SpringViewProvider viewProvider;
  @Autowired
  private TopNavBar topNavBar;
  @Autowired
  private LeftNavBar leftNavBar;
  @Autowired
  private PageTitleUpdater pageTitleUpdater;
  @Autowired
  private NavBarButtonUpdater navBarButtonUpdater;

  private EventBus eventBus;

  public static MainUI getCurrent() {
    return (MainUI) UI.getCurrent();
  }

  public static EventBus getEventBus() {
    return getCurrent().eventBus;
  }

  @Override
  protected void init(VaadinRequest request) {
    initEventBus();
    initMainContent();
    logger.info("Finished initialization of main UI");
  }

  private void initEventBus() {
    eventBus = new EventBus((throwable, subscriberExceptionContext) -> {
      logger.error("Subscriber event error: ", throwable);
    });
    eventBus.register(this);
  }

  private void initMainContent() {

    HorizontalLayout contentLayout = new HorizontalLayout();
    contentLayout.setSizeFull();
    setContent(contentLayout);

    contentLayout.addComponent(leftNavBar);

    VerticalLayout viewLayout = new VerticalLayout();
    viewLayout.setSizeFull();
    contentLayout.addComponent(viewLayout);
    contentLayout.setExpandRatio(viewLayout, 1.0f);

    viewLayout.addComponent(topNavBar);

    VerticalLayout viewContainer = new VerticalLayout();
    viewContainer.setSizeFull();
    viewLayout.addComponent(viewContainer);
    viewLayout.setExpandRatio(viewContainer, 1.0f);

    Navigator navigator = new Navigator(this, viewContainer);
    navigator.addProvider(viewProvider);
    navigator.addViewChangeListener(navBarButtonUpdater);
    navigator.addViewChangeListener(pageTitleUpdater);
  }

  @Subscribe
  public void navigateTo(UINavigationEvent view) {
    getNavigator().navigateTo(view.getViewName());
  }

  @Subscribe
  public void userLoggedIn(UILoginEvent event) {
    CurrentAccount.set(event.getAccount());
    topNavBar.updateUserLabel(event.getAccount().getFirstName());
    navBarButtonUpdater.changeButtonCaption(TopNavBar.SIGNIN_BUTTON_URI, TopNavBar.SIGNOUT_CAPTION);
  }

  @Subscribe
  public void logout(UILogoutEvent event) {
    // Don't invalidate the underlying HTTP session if you are using it for something else
    getPage().setLocation("/"); // redirect to the Home page
    VaadinSession.getCurrent().getSession().invalidate();
    VaadinSession.getCurrent().close();
  }
}