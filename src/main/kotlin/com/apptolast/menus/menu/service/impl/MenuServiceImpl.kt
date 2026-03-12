package com.apptolast.menus.menu.service.impl

import com.apptolast.menus.allergen.model.enum.AllergenType
import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.AllergenMatrixResponse
import com.apptolast.menus.menu.dto.response.AllergenMatrixRow
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuRecipe
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRecipeRepository
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.menu.service.MenuService
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.recipe.service.RecipeAllergenCalculator
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@Transactional
class MenuServiceImpl(
    private val menuRepository: MenuRepository,
    private val menuSectionRepository: MenuSectionRepository,
    private val menuRecipeRepository: MenuRecipeRepository,
    private val recipeRepository: RecipeRepository,
    private val recipeAllergenCalculator: RecipeAllergenCalculator
) : MenuService {

    private val logger = LoggerFactory.getLogger(MenuServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByRestaurant(restaurantId: UUID, includeArchived: Boolean): List<MenuResponse> {
        val menus = if (includeArchived)
            menuRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId)
        else
            menuRepository.findByRestaurantIdAndIsArchivedFalseOrderByDisplayOrderAsc(restaurantId)
        return menus.map { it.toResponse() }
    }

    override fun create(restaurantId: UUID, tenantId: UUID, request: MenuRequest): MenuResponse {
        val menu = Menu(
            restaurantId = restaurantId,
            tenantId = tenantId,
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder
        )
        return menuRepository.save(menu).toResponse()
    }

    override fun update(id: UUID, tenantId: UUID, request: MenuRequest): MenuResponse {
        val menu = findMenuOrThrow(id)
        menu.name = request.name
        menu.description = request.description
        menu.displayOrder = request.displayOrder
        menu.updatedAt = OffsetDateTime.now()
        return menuRepository.save(menu).toResponse()
    }

    override fun archive(id: UUID, tenantId: UUID) {
        val menu = findMenuOrThrow(id)
        menu.isArchived = true
        menu.isPublished = false
        menu.updatedAt = OffsetDateTime.now()
        menuRepository.save(menu)
    }

    override fun addSection(menuId: UUID, tenantId: UUID, request: SectionRequest): SectionResponse {
        val menu = findMenuOrThrow(menuId)
        val section = MenuSection(
            menu = menu,
            tenantId = tenantId,
            name = request.name,
            displayOrder = request.displayOrder
        )
        return menuSectionRepository.save(section).toResponse()
    }

    override fun updateSection(sectionId: UUID, tenantId: UUID, request: SectionRequest): SectionResponse {
        val section = menuSectionRepository.findById(sectionId)
            .orElseThrow { ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found") }
        section.name = request.name
        section.displayOrder = request.displayOrder
        return menuSectionRepository.save(section).toResponse()
    }

    override fun deleteSection(sectionId: UUID, tenantId: UUID) {
        if (!menuSectionRepository.existsById(sectionId)) {
            throw ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found")
        }
        menuSectionRepository.deleteById(sectionId)
    }

    override fun publish(menuId: UUID, tenantId: UUID, published: Boolean): MenuResponse {
        val menu = findMenuOrThrow(menuId)
        menu.isPublished = published
        if (published) {
            menu.isArchived = false
        }
        menu.updatedAt = OffsetDateTime.now()
        return menuRepository.save(menu).toResponse()
    }

    override fun addRecipeToMenu(menuId: UUID, tenantId: UUID, recipeId: UUID, section: String, sortOrder: Int): MenuRecipe {
        findMenuOrThrow(menuId)
        val recipe = recipeRepository.findById(recipeId)
            ?: throw ResourceNotFoundException("RECIPE_NOT_FOUND", "Recipe not found")

        if (menuRecipeRepository.existsByMenuIdAndRecipeId(menuId, recipeId)) {
            throw ConflictException("MENU_RECIPE_EXISTS", "Recipe is already added to this menu")
        }

        val menuRecipe = MenuRecipe(
            menuId = menuId,
            recipeId = recipeId,
            sectionName = section,
            sortOrder = sortOrder,
            tenantId = tenantId
        )
        return menuRecipeRepository.save(menuRecipe)
    }

    override fun removeRecipeFromMenu(menuId: UUID, tenantId: UUID, recipeId: UUID) {
        findMenuOrThrow(menuId)
        if (!menuRecipeRepository.existsByMenuIdAndRecipeId(menuId, recipeId)) {
            throw ResourceNotFoundException("MENU_RECIPE_NOT_FOUND", "Recipe not found in this menu")
        }
        menuRecipeRepository.deleteByMenuIdAndRecipeId(menuId, recipeId)
    }

    @Transactional(readOnly = true)
    override fun getAllergenMatrix(menuId: UUID, tenantId: UUID): AllergenMatrixResponse {
        val menu = findMenuOrThrow(menuId)
        val menuRecipes = menuRecipeRepository.findByMenuIdOrderBySortOrderAsc(menuId)
        val allergenCodes = AllergenType.entries.map { it.name }

        val rows = menuRecipes.mapNotNull { menuRecipe ->
            val recipe = recipeRepository.findById(menuRecipe.recipeId) ?: return@mapNotNull null
            val computedAllergens = recipeAllergenCalculator.computeAllergens(menuRecipe.recipeId)
            val allergenMap = allergenCodes.associateWith { code ->
                computedAllergens.find { it.code == code }?.level ?: "FREE"
            }
            AllergenMatrixRow(
                recipeId = recipe.id,
                recipeName = recipe.name,
                section = menuRecipe.sectionName,
                allergens = allergenMap
            )
        }

        return AllergenMatrixResponse(
            menuName = menu.name,
            menuId = menu.id,
            allergenColumns = allergenCodes,
            rows = rows
        )
    }

    @Transactional(readOnly = true)
    override fun exportPdf(menuId: UUID, tenantId: UUID): ByteArray {
        val matrix = getAllergenMatrix(menuId, tenantId)
        val outputStream = ByteArrayOutputStream()

        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, outputStream)
        document.open()

        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, Color.BLACK)
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, Color.WHITE)
        val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7f, Color.BLACK)
        val legalFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.DARK_GRAY)

        val title = Paragraph("Allergen Matrix: ${matrix.menuName}", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 10f
        document.add(title)

        val dateParagraph = Paragraph(
            "Generated: ${OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}",
            legalFont
        )
        dateParagraph.alignment = Element.ALIGN_RIGHT
        dateParagraph.spacingAfter = 15f
        document.add(dateParagraph)

        val allergenAbbreviations = AllergenType.entries.map { it.abbreviation }
        val columnCount = 1 + allergenAbbreviations.size
        val table = PdfPTable(columnCount)
        table.widthPercentage = 100f

        val recipeColWidth = 4f
        val allergenColWidth = 1f
        val widths = FloatArray(columnCount) { i ->
            if (i == 0) recipeColWidth else allergenColWidth
        }
        table.setWidths(widths)

        val headerColor = Color(44, 62, 80)
        val recipeHeader = PdfPCell(Phrase("Recipe", headerFont))
        recipeHeader.backgroundColor = headerColor
        recipeHeader.horizontalAlignment = Element.ALIGN_CENTER
        recipeHeader.verticalAlignment = Element.ALIGN_MIDDLE
        recipeHeader.setPadding(4f)
        table.addCell(recipeHeader)

        for (abbreviation in allergenAbbreviations) {
            val cell = PdfPCell(Phrase(abbreviation, headerFont))
            cell.backgroundColor = headerColor
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.verticalAlignment = Element.ALIGN_MIDDLE
            cell.setPadding(3f)
            table.addCell(cell)
        }

        val containsColor = Color(231, 76, 60)
        val mayContainColor = Color(243, 156, 18)
        val freeColor = Color(46, 204, 113)
        val alternateRowColor = Color(245, 245, 245)

        for ((index, row) in matrix.rows.withIndex()) {
            val rowColor = if (index % 2 == 0) Color.WHITE else alternateRowColor
            val nameCell = PdfPCell(Phrase(row.recipeName, cellFont))
            nameCell.backgroundColor = rowColor
            nameCell.setPadding(4f)
            table.addCell(nameCell)

            for (allergenType in AllergenType.entries) {
                val level = row.allergens[allergenType.name] ?: "FREE"
                val (symbol, bgColor) = when (level) {
                    "CONTAINS" -> "X" to containsColor
                    "MAY_CONTAIN" -> "T" to mayContainColor
                    else -> "-" to freeColor
                }
                val cell = PdfPCell(Phrase(symbol, cellFont))
                cell.backgroundColor = bgColor
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.verticalAlignment = Element.ALIGN_MIDDLE
                cell.setPadding(3f)
                table.addCell(cell)
            }
        }

        document.add(table)

        val legend = Paragraph().apply {
            spacingBefore = 15f
            add(Phrase("Legend: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f)))
            add(Phrase("X = Contains  |  T = May contain (traces)  |  - = Free of allergen", legalFont))
        }
        document.add(legend)

        val legal = Paragraph(
            "This document complies with EU Regulation 1169/2011 on food allergen declaration. " +
                "The information provided is based on the ingredient data entered by the restaurant. " +
                "Cross-contamination risks may exist.",
            legalFont
        )
        legal.spacingBefore = 10f
        document.add(legal)

        document.close()
        return outputStream.toByteArray()
    }

    private fun findMenuOrThrow(id: UUID): Menu =
        menuRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("MENU_NOT_FOUND", "Menu not found") }

    private fun Menu.toResponse() = MenuResponse(
        id = id,
        name = name,
        description = description ?: "",
        archived = isArchived,
        displayOrder = displayOrder,
        sections = sections.map { it.toResponse() },
        updatedAt = updatedAt
    )

    private fun MenuSection.toResponse() = SectionResponse(
        id = id,
        name = name,
        displayOrder = displayOrder
    )
}
