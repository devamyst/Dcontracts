# READ ME!

This update has many changes, pleas read the changelog carefully before updating.

## Config Overhaul

The plugin should automatically migrate to the new formats for you.

Each GUI now has its own config file instead of putting everything in `config.yml`.
<br>
The config files of the GUIs are under `gui/` directory.

`gui.main` > `main.yml`
<br>
`gui.your-orders` > `your-orders.yml`
<br>
`gui.choose-item` > `choose-items.yml`
<br>
`gui.search-sign` > `search.yml`
<br>
`gui.delivery` > `deliver.yml`
<br>
`gui.enchant-item` > `enchant.yml`
<br>
`gui.new-order` > `new-order-dialog.yml`
<br>
`gui.confirm-delivery` > `confirm-delivery.yml`
<br>
`gui.manage-order` > `manage-order.yml`

In container GUIs, you can now specify the amount of rows.
<br>
For main, your orders, choose items, enchant item GUIs, you can now specify a list of slots where the
orders/items/enchantment books should be put in respectively.
<br>
Buttons now have 2 keys, `item` and `slot`; name, lore, item model of the button is now moved to the `item` key, under
`components`, providing a more vanilla format.
<br>`name-prefix` in enchant GUI's config is changed to `name.active` and `name.inactive`

`sort-prefix` and `sort-types` are now merged to `sorts-display`, allowing you to set the text for each sort type when
it is active and inactive.

## FastStats

The plugin now uses [FastStats](https://faststats.dev/) to collect anonymous data.
Besides default metrics it collects, Contracts also tracks errors at different places, and these data:

- API Usage: a boolean determines if your server is using API from Contracts
- Experimental features: a string array contains names of experimental features being used
- Order amount: an integer shows the amount of orders have been created
- Items collected: an integer shows the amount of items collected from all orders