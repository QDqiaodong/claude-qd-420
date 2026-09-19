package com.aquarium.tank.entity;

import com.aquarium.tank.dto.BizException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tank")
@Getter
@Setter
public class Tank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TankState state;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tank", fetch = jakarta.persistence.FetchType.EAGER)
    @JsonIgnore
    private List<Creature> creatures = new ArrayList<>();

    /** 当前在缸生物数（由级联集合聚合得出）。 */
    public int getCreatureCount() {
        return creatures == null ? 0 : creatures.size();
    }

    /** 状态机：正常 -> 维护。 */
    public void startMaintenance() {
        if (this.state != TankState.NORMAL) {
            throw new BizException("仅「正常」展缸可进入维护");
        }
        this.state = TankState.MAINTENANCE;
    }

    /** 状态机：维护 -> 正常。 */
    public void finishMaintenance() {
        if (this.state != TankState.MAINTENANCE) {
            throw new BizException("仅「维护」中展缸可结束维护");
        }
        this.state = TankState.NORMAL;
    }

    /** 停机前置校验：有生物时禁止停用。 */
    public void assertCanDisable() {
        if (!creatures.isEmpty()) {
            throw new BizException("有生物时禁止停用展缸，请先下架并移除全部生物");
        }
    }

    /** 状态机：正常/维护 -> 停用（需先清空生物）。 */
    public void disable() {
        assertCanDisable();
        if (this.state == TankState.DISABLED) {
            throw new BizException("展缸已停用");
        }
        this.state = TankState.DISABLED;
    }
}
