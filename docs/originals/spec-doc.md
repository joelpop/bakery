# CASE

*The Bakery* owns a centrally located café in addition to the actual bakery, which in turn is located outside the city center.

In addition to daily producing a standard set of baked goods that can be bought fresh from the café or straight from the bakery, the Bakery is also the most well-known producer of cakes for special occasions—offering customization to a fairly wide variety of cakes in a few different sizes.

Most people just know (by rumor and experience) that you can order the cakes from the café and go pick them up at the agreed upon date.

People don’t reflect much on where (or when) the cakes are made, but the personnel is acutely aware; they need to get correct orders to the bakery so the customer gets what they ordered on the correct date for their event—and that event might be as simple as a workplace meeting, but quite often it’s a special occasion—like a birthday or wedding.

Orders need to be made just-in-time, i.e., the morning of the day it is due, and messing up the order is no joke.

Unfortunately, the finer details and limitations of the bakery is sometimes learned the hard way, and the current process is based on handwritten notes passed along as photos in WhatsApp.

The Bakery needs an application to handle the order fulfillment for custom cakes, to ensure orders are correctly received, baked to spec, and available the correct day.

# PERSONAS

| Name  | Role     | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|-------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Peter | Customer | From time to time, Peter contacts a Barista (or occasionally a Baker) with the intention to place an order. He has used the service several times, both personally (family occasions) or representing his workplace (personnel occasions).  Often he shows up in person and explains the situation (occasion, number of guests, etc.) to get the suggestions he can pick from—but lately he has also phoned in some orders for repeating events at work. The most important thing to Peter is that he gets something of suitable size on the right day—though he’ll be disappointed if it’s not exactly what he ordered, it will not totally ruin his day.                                                                                                                                                                                                                                                                         |
| Malin | Barista  | Works at the brick-and-mortar storefront serving customers, which also involves taking orders from customers like Peter—in person or over the phone (sometimes with a queue of customers waiting impatiently at the store).  The old paper-based system is error-prone and causes significant stress for Malin during busy periods; she needs to get the correct information, both about the customer and the order, then send it to the bakery—unfortunately there is little she can do to verify the order has been correctly received until the customer shows up asking for his order. Also, with more customized or uncommon orders, it’s hard to know if the bakery has the ability to produce the order in the given timeframe, since the inner workings of the bakery are not familiar to Malin, and she has no way of knowing if some ingredient is running low. The communication regarding orders is basically one-way. |
| Heidi | Baker    | At the bakery, orders are received mainly from the Baristas, but also directly from customers. Orders are reviewed and scheduled by the Bakers, so they can be freshly made just-in-time.  Most mishaps are related to misunderstanding or misreading the handwritten orders, but sometimes also due to ingredients running low, especially for more uncommon products. Some things also take extra time to prepare and are impossible to produce on very short notice. Heidi feels it’s the responsibility of the Barista to not accept last-minute orders for such products. However, she is reluctant to document the “rules,” because it’s kind of hard to put on paper, though it is fairly obvious for everyone working at the bakery.                                                                                                                                                                                       |

# JOBS TO BE DONE

## Contexts

The customer does not interact directly with the system, leaving three main contexts:

1. Storefront
    1. Accepting orders
    2. Delivering (handing over) orders being picked up by the customer
2. Bakery
    1. Making orders that are due that day
    2. Reviewing/verifying incoming (new) orders for following days
    3. Secondary: accepting orders
3. Back-Office (located at the bakery but identified as a separate function)
    1. Status / overview (decision-making, QA)
    2. System administration
    3. Secondary: accepting phone orders

The interactions with the system at the bakery basically consist of reviewing and changing the current state of orders. Let's work with the initial assumption that this context can be sufficiently addressed by the other two contexts; not requiring its own screens. This assumption should be user-tested.

## Jobs NOT to be done

* No online store—customer only plays an indirect role.
* No deliveries or shipping—orders are always picked up by the customer.
* No inventory management or functions to aid in manufacturing.
* No quick user switching—users are assumed to use personal devices or joint account (at the store)

## Jobs to be done

### Storefront

* Accept orders
    * Customer info
        * Name
        * Phone number
        * For companies
            * Billing info
        * Notes?
    * Multiple items
        * Specs
            * Type
            * Quantity
            * Customizations
        * Due-date
        * Price
        * Pick-up location
* Verify order
    * Show a summary to the customer
        * Phone
        * Products, size, pieces, customizations
        * Due-date
        * Pick-up location
* Place order
* View status
    * List all orders, due-dates, status
* Handle pickup
    * Find order
    * Mark as picked up
    * Mark as paid

### Back-Office

* View status
* View statistics
* (Secondary) Accept order

### Bakery

* Verify orders
* View today's and tomorrow's orders
* Problem alert—customer should be contacted
    * Way of recording the problem (status / note)
    * Way of recording resolution of the problem (status / note)
* Start making item (status change)
* Ready (status change)
* Delivered to store (status change)

### System

* Direct web link to orders
    * To be able to talk about orders, “Take a look at order \#234”

# DASHBOARD DATA

* Soon due orders
* Alerts / bulletin board
* Stats
    * Upcoming
    * Incoming
    * Warnings / failures
    * Month \+ delta
    * Year \+ delta
    * Product share

# ENTITY NOTES

* User (Customer? / Employee / Admin / Disabled)
    * Name, phone, additional details
    * Customer has no access, for now only used to autofill contact details and log
        * \-\> CAN potentially be separate?
    * Employee
        * cannot access Product administration
        * cannot change User role
    * Admin can do all the things
* Product
    * Name, description, price, available
* Order Details
  * Due date
  * Items
      * Product
      * Pcs
      * Specs (decoration etc)
  * Price
  * Discount?
  * State
      * New
      * Verified
      * Not OK 		(something wrong, needs attention)
      * Canceled 	(canceled on purpose, no further action needed)
      * In progress (a.k.a manufacturing)
      * Ready 		(at bakery)
      * In store 	(available for pickup)
      * Delivered 	(picked up)
  * Paid (bool) _In rare cases, the order can be picked up, but not paid (e.g to be billed) or (even more seldom) wise versa. Hence Paid is a separate property._

# FLOWS

## Activities

### Login

_All activities require the user to be logged in._

### Sell

_Sell needs the most attention to streamline entry and avoid errors._

New order \-\> Add due date \-\> Choose items and details \-\> Enter (autofill) customer details \-\> Confirm

#### Notes

* Entering a new order should be fast regardless of which state the application currently is in. (Accessible from all screens.)
* Due date is the first detail to be able to verify the feasibility of the order right away (Due tomorrow, and it’s evening now? Maybe no can do. Special order for the day after tomorrow? Might need to double-check with bakery.)
* Autocompleting existing customer info based on phone / name speeds up the process and avoids mistakes.
* Autocomplete products. No separate product page accessible at this point in the order entry; customer and order-taker will have to look at an offline catalog of products.

### Make and Deliver

_Make and deliver are mostly about changing the state of the order._

Find orders due \-\> Mark ‘in progress’ \-\> Mark ‘finished’ \-\> Mark ‘in store’

### View Statistics and Admin

_These are activities that manager-type persons are interested in, perhaps not even on a daily basis. (However, to allow mistakes and exceptional situations to be spotted by everyone, some form of readily available overview is useful for everyone.)_

* View statistics
* Administer users
* Administer products

# RESPONSIVENESS

There are three main target sizes; phone, tablet, and desktop.  
However, the desktop design is mainly a slightly wide tablet view with an additional (permanent) application menu on the top.

# SCREEN NOTES

* Store dashboard
    * Due list
    * Today done/pending?
    * N/A due today
    * Tomorrow pending?
    * Bulletin
    * New \-\>
* Back-Office dashboard
    * Store \+
    * Monthly
    * Yearly
    * Failures
    * Admin (users, customers) \-\>
    * New \-\>
* New order
    * Due date
    * Choose products
        * Specs
    * Note
    * Customer
    * Price
* Order details
    * New order \+
    * Paid
    * Current state
    * Next state \-\>
    * Update state \-\>
    * History
