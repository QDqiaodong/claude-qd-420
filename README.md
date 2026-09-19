# 水族馆管理系统

水族馆（展缸 / 生物 / 投喂 / 维生巡检）的台账与运维管理。基于 Spring Boot 3.3 (JPA) + Vue 3 + MySQL + Redis，docker compose 一键启动。

后端核心差异化：展缸 `Tank` 与生物 `Creature` 采用 `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` 级联关系；展缸状态用枚举 `TankState`（正常/维护/停用），状态机前进逻辑写在实体方法里（`startMaintenance()`、`finishMaintenance()`、`assertCanDisable()`），由 Service 调用。

## 启动

```bash
./start.sh
```

启动后访问 http://127.0.0.1:8250/ 。

## 模块

- 展缸：展缸台账，含容量/生物数/状态枚举色标，点按钮切状态（正常→维护→正常；正常→停用需先清空生物）
- 生物：生物档案，归属展缸（须正常）、品种、在展/下架状态
- 投喂：按展缸按日的投喂登记，维护/停用展缸禁止投喂
- 维生巡检：巡检点位列表，异常必须填备注
