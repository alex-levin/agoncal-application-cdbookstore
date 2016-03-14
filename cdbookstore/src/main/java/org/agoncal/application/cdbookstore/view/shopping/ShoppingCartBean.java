package org.agoncal.application.cdbookstore.view.shopping;

import org.agoncal.application.cdbookstore.model.*;
import org.agoncal.application.cdbookstore.view.account.AccountBean;
import org.agoncal.application.invoice.model.Invoice;
import org.agoncal.application.invoice.model.InvoiceLine;

import javax.annotation.Resource;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSSessionMode;
import javax.jms.Queue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Antonio Goncalves http://www.antoniogoncalves.org --
 */

@Named
@SessionScoped
public class ShoppingCartBean implements Serializable {

    // ======================================
    // =          Injection Points          =
    // ======================================

    @Inject
    private FacesContext facesContext;

    @Inject
    private AccountBean accountBean;

    @Inject
    private transient JMSContext context;

    @Resource(lookup = "jms/queue/invoiceQueue")
    private Queue queue;


    @PersistenceContext(unitName = "applicationCDBookStorePU")
    private EntityManager em;

    // ======================================
    // =             Attributes             =
    // ======================================

    private List<ShoppingCartItem> cartItems = new ArrayList<>();
    private Address address = new Address();
    private CreditCard creditCard = new CreditCard();

    // ======================================
    // =          Business methods          =
    // ======================================

    public String addItemToCart() {
        Item item = em.find(Item.class, getParamId("itemId"));

        boolean itemFound = false;
        for (ShoppingCartItem cartItem : cartItems) {
            // If item already exists in the shopping cart we just change the quantity
            if (cartItem.getItem().equals(item)) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                itemFound = true;
            }
        }
        if (!itemFound)
            // Otherwise it's added to the shopping cart
            cartItems.add(new ShoppingCartItem(item, 1));

        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, item.getTitle() + " added to the shopping cart",
                "You can now add more stuff if you want"));

        return "/shopping/viewItem.xhtml?faces-redirect=true&includeViewParams=true";
    }

    public String removeItemFromCart() {
        Item item = em.find(Item.class, getParamId("itemId"));

        for (ShoppingCartItem cartItem : cartItems) {
            if (cartItem.getItem().equals(item)) {
                cartItems.remove(cartItem);
                return null;
            }
        }

        return null;
    }

    public String updateQuantity() {
        return null;
    }

    public String checkout() {
        return "/checkout";
    }

    public String confirmation() {

        // Creating the invoice
        User user = accountBean.getUser();
        Invoice invoice = new Invoice(user.getFirstName(), user.getLastName(), user.getEmail());
        for (ShoppingCartItem cartItem : cartItems) {
            invoice.addInvoiceLine(new InvoiceLine(cartItem.getQuantity(), cartItem.getItem().getTitle(), cartItem.getItem().getUnitCost()));
        }

        // Sending the invoice
        context.createProducer().setTimeToLive(1000).send(queue, invoice);

        // Displaying the invoice creation
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Order created",
                "You will receive a confirmation email"));

        return "/main";
    }

    public List<ShoppingCartItem> getCartItems() {
        return cartItems;
    }

    public boolean shoppingCartIsEmpty() {
        return getCartItems() == null || getCartItems().size() == 0;
    }

    public Float getTotal() {
        if (cartItems == null || cartItems.isEmpty())
            return 0f;

        Float total = 0f;

        // Sum up the quantities
        for (ShoppingCartItem cartItem : cartItems) {
            total += (cartItem.getSubTotal());
        }
        return total;
    }

    protected Long getParamId(String param) {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> map = context.getExternalContext().getRequestParameterMap();
        return Long.valueOf(map.get(param));
    }

    // ======================================
    // =        Getters and Setters         =
    // ======================================

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public CreditCardType[] getCreditCardTypes() {
        return CreditCardType.values();
    }
}