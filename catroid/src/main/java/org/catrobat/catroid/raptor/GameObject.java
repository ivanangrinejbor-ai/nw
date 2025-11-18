package org.catrobat.catroid.raptor;

import java.util.ArrayList;
import java.util.List;

public class GameObject {
    public String id;
    public String name;
    public boolean isActive = true;
    public TransformComponent transform = new TransformComponent();
    public List<Component> components = new ArrayList<>();

    public String parentId = null;
    public List<String> childrenIds = new ArrayList<>();

    public GameObject() {}

    public GameObject(String name) {
        this.id = name;
        this.name = name;
    }

    public boolean addComponent(Component component) {
        components.add(component);
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> type) {
        for (Component c : components) {
            if (type.isInstance(c)) {
                return (T) c;
            }
        }
        return null;
    }

    public <T extends Component> boolean hasComponent(Class<T> type) {
        return getComponent(type) != null;
    }

    public <T extends Component> List<T> getComponents(Class<T> type) {
        List<T> foundComponents = new ArrayList<>();
        for (Component c : components) {
            if (type.isInstance(c)) {
                foundComponents.add((T) c);
            }
        }
        return foundComponents;
    }
}