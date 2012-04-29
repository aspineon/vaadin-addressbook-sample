package com.vobject.vaadin.addressbook.sample;

import com.vaadin.Application;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Window.Notification;
import com.vobject.vaadin.addressbook.sample.data.PersonContainer;
import com.vobject.vaadin.addressbook.sample.data.SearchFilter;
import com.vobject.vaadin.addressbook.sample.ui.HelpWindow;
import com.vobject.vaadin.addressbook.sample.ui.ListView;
import com.vobject.vaadin.addressbook.sample.ui.NavigationTree;
import com.vobject.vaadin.addressbook.sample.ui.PersonForm;
import com.vobject.vaadin.addressbook.sample.ui.PersonList;
import com.vobject.vaadin.addressbook.sample.ui.SearchView;
import com.vobject.vaadin.addressbook.sample.ui.SharingOptions;

public class VaadinAddressBookApplication extends Application implements
		Button.ClickListener, Property.ValueChangeListener, ItemClickListener {
	private Button newContact = new Button("Add contact");
	private Button search = new Button("Search");
	private Button share = new Button("Share");
	private Button help = new Button("Help");
	private HorizontalSplitPanel horizontalSplit = new HorizontalSplitPanel();
	private NavigationTree tree = new NavigationTree(this);
	private ListView listView = null;
	private PersonList personList = null;
	private PersonForm personForm = null;	
	private HelpWindow helpWindow = null;
	private PersonContainer personDataSource = PersonContainer.createWithTestData();

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3591316946318123623L;

	@Override
	public void init() {
		createMainLayout();
	}

	private void createMainLayout() {
		// setTheme("contacts");
		Window mainWindow = new Window("Vaadin Address Book Sample Application");
		setMainWindow(mainWindow);
		VerticalLayout layout = new VerticalLayout();
		layout.setHeight("500");

		layout.addComponent(createToolbar());
		layout.addComponent(horizontalSplit);

		/* Allocate all available extra space to the horizontal split panel */
		layout.setExpandRatio(horizontalSplit, 1);

		/*
		 * Set the initial split position so we can have a 200 pixel menu to the
		 * left
		 */
		horizontalSplit.setSplitPosition(150, HorizontalSplitPanel.UNITS_PIXELS);
		
		horizontalSplit.setFirstComponent(tree);
		
		setMainComponent(getListView());
		
		mainWindow.setContent(layout);
	}
	
	// lazy initialization pattern
	private ListView getListView() {
		if (listView == null) {
			personList = new PersonList(this);
			personForm = new PersonForm(this);
			listView = new ListView(personList, personForm);
		}
		return listView;
	}

	// lazy initialization pattern
	private HelpWindow getHelpWindow() {
		if (helpWindow == null) {
			helpWindow = new HelpWindow();
		}
		return helpWindow;
	}	
	
	SharingOptions sharingOptions = null;
	
	private SharingOptions getSharingOptions() {
		if (sharingOptions == null) {
			sharingOptions = new SharingOptions();
		}
		return sharingOptions;
	}	
	
	private SearchView searchView = null;

	private SearchView getSearchView() {
		if (searchView == null) {
			searchView = new SearchView(this);
		}
		return searchView;
	}

	private HorizontalLayout createToolbar() {
		HorizontalLayout lo = new HorizontalLayout();

		newContact.setIcon(new ThemeResource("icons/32/document-add.png"));
		newContact.addListener((Button.ClickListener) this);
		lo.addComponent(newContact);
		search.setIcon(new ThemeResource("icons/32/folder-add.png"));
		search.addListener((Button.ClickListener) this);
		lo.addComponent(search);
		share.setIcon(new ThemeResource("icons/32/users.png"));
		share.addListener((Button.ClickListener) this);
		lo.addComponent(share);
		help.setIcon(new ThemeResource("icons/32/help.png"));
		help.addListener((Button.ClickListener) this);
		lo.addComponent(help);

		lo.setMargin(true);
		lo.setSpacing(true);
		lo.setWidth("100%");

		Embedded em = new Embedded("", new ThemeResource("images/logo.png"));
		lo.addComponent(em);
		lo.setComponentAlignment(em, Alignment.MIDDLE_RIGHT);
		lo.setExpandRatio(em, 1);
		lo.setStyleName("toolbar");
		return lo;
	}
	
	private void showSearchView() {
	    setMainComponent(getSearchView());
	}
	
	private void setMainComponent(Component c) {
		horizontalSplit.setSecondComponent(c);
	}

	public PersonContainer getPersonDataSource() {
		return personDataSource;
	}

	public void valueChange(ValueChangeEvent event) {
		Property property = event.getProperty();
		if (property == personList) {
			Item item = personList.getItem(personList.getValue());
			if (item != personForm.getItemDataSource()) {
				personForm.setItemDataSource(item);
			}
		}
	}

	public void buttonClick(ClickEvent event) {
		final Button source = event.getButton();
		if (source == search) {
			showSearchView();
		} else if (source == newContact) {
			addNewContanct();
		} else if (source == help) {
			getMainWindow().addWindow(getHelpWindow());
		} else if (source == share) {
			getMainWindow().addWindow(getSharingOptions());
		}
	}

	public void itemClick(ItemClickEvent event) {
		if (event.getSource() == tree) {
			Object itemId = event.getItemId();
			if (itemId != null) {
				if (NavigationTree.SHOW_ALL.equals(itemId)) {
					// clear previous filters
					getPersonDataSource().removeAllContainerFilters();
					showListView();
				} else if (NavigationTree.SEARCH.equals(itemId)) {
					showSearchView();
				} else if (itemId instanceof SearchFilter) {
					search((SearchFilter) itemId);
				}
			}
		}
	}

	private void showListView() {
		setMainComponent(getListView());
	}
	
	private void addNewContanct() {
		showListView();
		personForm.addContact();
	}

	public void saveSearch(SearchFilter searchFilter) {
		tree.addItem(searchFilter);
		tree.setParent(searchFilter, NavigationTree.SEARCH);
		// mark the saved search as a leaf (cannot have children)
		tree.setChildrenAllowed(searchFilter, false);
		// make sure "Search" is expanded
		tree.expandItem(NavigationTree.SEARCH);
		// select the saved search
		tree.setValue(searchFilter);
	}

	public void search(SearchFilter searchFilter) {
		// clear previous filters
		getPersonDataSource().removeAllContainerFilters();
		// filter contacts with given filter
		getPersonDataSource().addContainerFilter(searchFilter.getPropertyId(),
				searchFilter.getTerm(), true, false);
		showListView();
		showNotification(searchFilter);
	}

	private void showNotification(SearchFilter searchFilter) {
		if (getPersonDataSource().size() > 0) {
			getMainWindow().showNotification(
					"Searched for " + searchFilter.getPropertyId() + "=*"
							+ searchFilter.getTerm() + "*, found "
							+ getPersonDataSource().size() + " item(s).",
					Notification.TYPE_TRAY_NOTIFICATION);
		} else {
			getMainWindow().showNotification(
					"Searched for " + searchFilter.getPropertyId() + "=*"
							+ searchFilter.getTerm() + "*, person not found.",
					Notification.TYPE_TRAY_NOTIFICATION);
		}
	}
}
