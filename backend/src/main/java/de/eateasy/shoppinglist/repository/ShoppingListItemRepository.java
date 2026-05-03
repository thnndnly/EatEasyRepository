package de.eateasy.shoppinglist.repository;

import de.eateasy.shoppinglist.entity.ShoppingListItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ShoppingListItemRepository implements PanacheRepositoryBase<ShoppingListItem, UUID> {
}
