package com.bdozer.tag

import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("api/tags")
class TagController(private val tagService: TagService) {
    @GetMapping
    fun findTag(@RequestParam term: String): List<Tag> {
        return tagService.findTag(term)
    }

    @PostMapping
    fun saveTag(@RequestBody tag: Tag) {
        return tagService.saveTag(tag)
    }

    @DeleteMapping("{id}")
    fun deleteTag(@PathVariable id: String) {
        return tagService.deleteTag(id)
    }
}