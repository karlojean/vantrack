package com.vantrack.routes;

import com.vantrack.vans.Van;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "van_id", nullable = false)
    private Van van;

    @NonNull
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    private Set<UserRoute> userRoutes = new LinkedHashSet<>();

    public void addParent(UserRoute userRoute) {
        userRoutes.add(userRoute);
        userRoute.setRoute(this);
    }
}