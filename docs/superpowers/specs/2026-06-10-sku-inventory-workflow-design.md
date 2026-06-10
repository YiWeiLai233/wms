# SKU Inventory Workflow Design

## Goal

Rebuild the inventory entry workflow so SKU management becomes the main place to create SKUs, perform initial inbound stock, and view inventory. The existing stock query page will no longer be an inbound entry point and will be hidden from primary navigation in this iteration.

## Current Problems

- SKU creation and manual inbound are split across two pages, so users must create a SKU first and then go to stock query to enter stock.
- `product_sku.quantity` duplicates the meaning of real inventory in `stock.quantity`, which can make SKU lists and inventory queries disagree.
- The current backend auto-inbound logic chooses the first location under the product shelf and silently skips inbound when the product, shelf, or location is missing.
- The stock query page still follows an older warehouse-area-shelf-location flow, while shelves now belong directly to warehouses.

## Recommended Approach

Use SKU management as the inventory-facing product page.

- The SKU list displays SKU fields plus inventory summary: total stock, available stock, locked stock, defective stock, and low/out-of-stock status.
- Creating a SKU can include an initial inbound section with warehouse, shelf, location, quantity, and remark.
- The backend creates the SKU and stock record in one transaction. If initial quantity is greater than zero, location is required and a stock log is written with `biz_type = INBOUND`.
- Stock query is removed from the primary navigation. It should not expose an inbound button in this iteration.
- Stock log and stock check pages remain because they serve audit and warehouse operations.

## Page Behavior

### SKU Management

The SKU table becomes the main inventory overview:

- Basic columns: SKU code, SKU name, product name, shelf/category, prices, status.
- Inventory columns: available quantity, locked quantity, defective quantity, total quantity.
- Actions: edit SKU, delete SKU, view inventory detail.

The create dialog adds an "initial inbound" section:

- Product, SKU code, SKU name, prices, weight, status remain in the basic section.
- Initial inbound fields are shown for new SKUs:
  - warehouse
  - shelf
  - location
  - initial quantity
  - remark
- When initial quantity is `0`, location can stay empty and no stock log is written.
- When initial quantity is greater than `0`, warehouse, shelf, and location are required.

The edit dialog should edit SKU metadata only. Stock changes after creation should go through future explicit stock adjustment workflows or existing audited flows, not through SKU metadata edits.

### Navigation

Hide the stock query menu item from the sidebar for this iteration. Keep:

- SKU management
- stock log
- stock check

This avoids duplicated inventory screens while preserving audit and counting workflows.

## Backend Design

### Data Source Of Truth

`stock` is the source of truth for inventory quantities.

`product_sku.quantity` should no longer drive displayed inventory. To reduce risk in this iteration, leave the column in place if the database already has it, but stop using it as the authoritative stock value in new SKU inventory summaries.

### SKU Create Request

Extend `ProductSkuSaveDTO` with initial inbound fields:

- `initialQuantity`
- `warehouseId`
- `shelfId`
- `locationId`
- `inboundRemark`

`quantity` remains only for backward compatibility during the transition. New frontend code should send `initialQuantity`.

### Transaction Flow

`ProductSkuServiceImpl.create` should:

1. Validate SKU code uniqueness.
2. Validate the product exists.
3. Insert `product_sku`.
4. If initial inbound quantity is greater than zero:
   - validate warehouse and location are present
   - validate location belongs to the selected shelf
   - validate shelf belongs to the selected warehouse
   - create or increment `stock` for SKU + location
   - write `stock_log` with `biz_type = INBOUND`
5. Return the SKU id.

No silent skip is allowed. If the user requested initial inbound and the location chain is invalid, the whole SKU creation fails and the transaction rolls back.

### SKU List Response

Extend SKU list data with inventory summary fields:

- `availableQty`
- `lockedQty`
- `defectiveQty`
- `totalQty`

The values are aggregated from `stock` by SKU id.

## Error Handling

- Duplicate SKU code returns a business error.
- Initial quantity less than zero returns a validation error.
- Initial quantity greater than zero without a location returns a validation error.
- Location/shelf/warehouse mismatch returns a business error.
- Any failure after SKU insert rolls back both SKU and inventory writes.

## Testing

Backend tests should cover:

- Creating a SKU with initial inbound writes `product_sku`, `stock`, and `stock_log`.
- Creating a SKU with zero initial quantity writes no stock log.
- Invalid location chain fails and rolls back SKU creation.
- SKU list inventory summary is read from `stock`, not `product_sku.quantity`.

Frontend verification should cover:

- SKU list shows inventory summary columns.
- New SKU dialog requires location only when initial quantity is greater than zero.
- New SKU save sends initial inbound fields.
- Stock query entry is hidden from navigation, while stock log and stock check remain.

## Out Of Scope

- Removing the `product_sku.quantity` database column.
- Building a full standalone stock adjustment page.
- Changing outbound, return, or stock check business rules.
- Reworking historical stock data.
