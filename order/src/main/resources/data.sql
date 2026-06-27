-- Existing demo orders aligned with the curated catalog.
-- not collected, not returned, not collect overdue, not return overdue
insert into book_order(user_id,book_id,book_reference_id,ordered_at,return_by,collected_at,returned_at,collect_by,book_isbn,book_name)
values (2,2,'2b-1c-2',DATEADD('DAY',-6, CURRENT_DATE),DATEADD('MONTH',1, CURRENT_DATE),null,null,DATEADD('DAY',3, CURRENT_DATE),'9787302000020','Effective Java');

-- not collected, not returned, collect overdue
insert into book_order(user_id,book_id,book_reference_id,ordered_at,return_by,collected_at,returned_at,collect_by,book_isbn,book_name)
values (2,4,'4b-2c-2',DATEADD('DAY',-7, CURRENT_DATE),DATEADD('DAY',25, CURRENT_DATE),null,null,DATEADD('DAY',-3, CURRENT_DATE),'9787302000044','Harry Potter and the Philosopher''s Stone');

-- collected, not returned, not return overdue
insert into book_order(user_id,book_id,book_reference_id,ordered_at,return_by,collected_at,returned_at,collect_by,book_isbn,book_name)
values (2,3,'3b-4c-2',DATEADD('DAY',-5, CURRENT_DATE),DATEADD('DAY',25, CURRENT_DATE),DATEADD('DAY',-3, CURRENT_DATE),null,DATEADD('DAY',-2, CURRENT_DATE),'9787302000037','Clean Code');

-- collected, not returned, return overdue
insert into book_order(user_id,book_id,book_reference_id,ordered_at,return_by,collected_at,returned_at,collect_by,book_isbn,book_name)
values (2,1,'1b-4c-2',DATEADD('DAY',-10, CURRENT_DATE),DATEADD('DAY',-2, CURRENT_DATE),DATEADD('DAY',-4, CURRENT_DATE),null,DATEADD('DAY',-3, CURRENT_DATE),'9787302000013','The Pragmatic Programmer');

-- returned order history for the admin/user history pages
insert into book_order_history(order_id,user_id,book_id,book_isbn,book_name,book_reference_id,ordered_at,collected_at,returned_at,late_fees)
values (1001,2,11,'9787302000112','Refactoring','11b-4c-2',DATEADD('DAY',-50, CURRENT_DATE),DATEADD('DAY',-48, CURRENT_DATE),DATEADD('DAY',-18, CURRENT_DATE),0);


