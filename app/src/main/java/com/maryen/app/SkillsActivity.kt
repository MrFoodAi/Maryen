package com.maryen.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SkillsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skills)

        val listView = findViewById<ListView>(R.id.skillsList)
        val skills = MaryenApp.instance.orchestratorSkills()
        val consent = MaryenApp.instance.consentGate

        val rows = skills.all().map { skill ->
            val status = if (!skill.requiresConsent) {
                "sempre attiva"
            } else if (consent.granted(skill.id)) {
                "attiva ✓"
            } else {
                "disattivata ✗ (tocca per abilitare)"
            }
            "${skill.label} — $status"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        listView.adapter = adapter

        val skillList = skills.all().toList()
        listView.setOnItemClickListener { _, _, position, _ ->
            val skill = skillList[position]
            if (skill.requiresConsent) {
                if (consent.granted(skill.id)) consent.revoke(skill.id) else consent.grant(skill.id)
                recreate()
            }
        }
    }
}
