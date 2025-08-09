package org.ratger.dump

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Other(private val plugin: JavaPlugin) : Listener {

    private val blockCenters = mutableListOf<Location>()

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.block.type == Material.RESPAWN_ANCHOR) {
            val player = event.player
            val block = event.block
            val location = block.location

            player.sendMessage("Воспроизводим партиклы")
            blockCenters.add(location)

            object : BukkitRunnable() {
                private var ticks = 0
                private var animationPhase = 3
                private var beamRadius = 0.0
                private var beamParticleCount = 10
                private var orbitTicks = 0
                private var orbitCycleCount = 0
                private var orbitRotationSpeed = 100
                private var orbitRadius = 2.0
                private var orbitHeight = 6.0
                private var orbitDirection = "right"

                override fun run() {
                    if (!blockCenters.contains(location)) {
                        cancel()
                        return
                    }

                    // Нет партиклов до активации
                    when (animationPhase) {
                        1 -> { // Партиклы при активации -> До открытия
                            if (ticks == 0) {
                                location.world?.playSound(
                                    location.clone().add(0.5, 0.5, 0.5),
                                    Sound.ENTITY_WARDEN_DEATH,
                                    2.0f,
                                    1.0f
                                )
                            }

                            val radius = 1.0
                            val particles = 15
                            val angleStep = 2 * Math.PI / particles

                            val randomIndex = Random.nextInt(particles)
                            val angle = randomIndex * angleStep
                            val x = radius * cos(angle)
                            val z = radius * sin(angle)
                            val particleLoc = location.clone().add(x + 0.5, 0.3, z + 0.5)

                            location.world?.spawnParticle(
                                Particle.SCULK_CHARGE,
                                particleLoc,
                                1,
                                0.0, 0.0, 0.0,
                                0.0,
                                0f
                            )

                            ticks++
                            if (ticks >= 100) {
                                ticks = 0
                                animationPhase = 2
                            }
                        }
                        2 -> { // Партиклы за 2 секунды до открытия -> До удаления
                            if (beamRadius == 0.0) {
                                location.world?.playSound(
                                    location.clone().add(0.5, 0.5, 0.5),
                                    Sound.ENTITY_WITHER_AMBIENT,
                                    2.0f,
                                    1.0f
                                )
                            }

                            if (beamRadius <= 10.0) {
                                spawnBeams(location.clone().add(0.5, 0.0, 0.5), beamRadius, beamParticleCount)
                                beamRadius += 0.2
                                beamParticleCount = (10 + (beamRadius * 10).toInt()).coerceAtMost(100)
                            }

                            val cycleTicks = orbitTicks % 240
                            val cycleProgress = cycleTicks / 240.0

                            val radius = when {
                                cycleProgress <= 0.25 -> orbitRadius * sin(Math.PI * (cycleProgress / 0.25))
                                cycleProgress <= 0.5 -> orbitRadius * sin(Math.PI * ((0.5 - cycleProgress) / 0.25))
                                cycleProgress <= 0.75 -> orbitRadius * sin(Math.PI * ((cycleProgress - 0.5) / 0.25))
                                else -> orbitRadius * sin(Math.PI * ((1.0 - cycleProgress) / 0.25))
                            }

                            val yOffset = when {
                                cycleProgress <= 0.5 -> orbitHeight * cycleProgress
                                else -> orbitHeight * (1.0 - cycleProgress)
                            }

                            if (cycleProgress == 0.0 || cycleProgress == 0.5 || cycleProgress == 1.0) {
                                orbitDirection = if (orbitDirection == "right") "left" else "right"
                            }

                            // Скорость & Плавность вращения
                            val angularSpeed = 2 * Math.PI / orbitRotationSpeed
                            val angle = if (orbitDirection == "right") {
                                angularSpeed * orbitTicks
                            } else {
                                -angularSpeed * orbitTicks
                            }

                            for (i in 0 until 4) {
                                val particleAngle = angle + (i * Math.PI / 2)
                                val x = radius * cos(particleAngle)
                                val z = radius * sin(particleAngle)
                                val particleLoc = location.clone().add(x + 0.5, -0.2+yOffset, z + 0.5)

                                location.world?.spawnParticle(
                                    Particle.SCULK_CHARGE_POP,
                                    particleLoc,
                                    1,
                                    0.0, 0.0, 0.0,
                                    0.0
                                )
                            }

                            orbitTicks++
                            if (orbitTicks >= 960) {
                                ticks = 0
                                beamRadius = 0.0
                                beamParticleCount = 10

                                orbitTicks = 0
                                orbitCycleCount = 0

                                animationPhase = 3
                            }
                        }
                        3 -> {
                            ticks++
                            if (ticks >= 40) {
                                ticks = 0
                                animationPhase = 1
                            }
                        }
                    }
                }

                private fun spawnBeams(center: Location, radius: Double, count: Int) {
                    for (i in 0 until count) {
                        val phi = (Math.PI / 2) * i / count
                        val theta = Math.PI * (1 + sqrt(5.0)) * i

                        val x = radius * sin(phi) * cos(theta)
                        val y = radius * cos(phi)
                        val z = radius * sin(phi) * sin(theta)

                        val particleLoc = center.clone().add(x,y,z)
                        val color = Color.fromRGB(0, 144, 147)
                        val dust = DustOptions(color, 1f)

                        center.world?.spawnParticle(
                            Particle.DUST,
                            particleLoc,
                            1,
                            0.15, 0.0, 0.15,
                            0.0,
                            dust
                        )

                        if (Random.nextFloat() < 0.4) {
                            center.world?.spawnParticle(
                                Particle.SCULK_CHARGE_POP,
                                particleLoc,
                                1,
                                0.0, 0.0, 0.0,
                                0.0
                            )
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.block.type == Material.RESPAWN_ANCHOR) {
            val location = event.block.location
            blockCenters.removeIf {it == location}
            event.player.sendMessage("Удаляем спавн партиклов")
        }
    }
}
