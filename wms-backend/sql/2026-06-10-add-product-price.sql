-- Add the product.price column expected by ProductMapper and the product form.
ALTER TABLE product
    ADD COLUMN price DECIMAL(10,2) DEFAULT 0.00 AFTER description;
