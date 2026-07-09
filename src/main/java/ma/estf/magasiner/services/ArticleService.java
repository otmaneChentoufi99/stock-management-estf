package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.models.dto.ArticleDto;
import ma.estf.magasiner.models.mapper.ArticleMapper;
import java.util.List;
import java.util.stream.Collectors;

public class ArticleService {
    private final ArticleDao dao = new ArticleDao();

    public List<ArticleDto> getAllArticles() {
        return dao.findAll().stream().map(ArticleMapper::toDto).collect(Collectors.toList());
    }

    public void createArticle(ArticleDto dto) {
        dao.save(ArticleMapper.toEntity(dto));
    }

    public void updateArticle(ArticleDto dto) {
        dao.update(ArticleMapper.toEntity(dto));
    }

    public Object[] getInventoryKpis() {
        return dao.getInventoryKpis();
    }

    public List<Object[]> getCategoryStockDistribution() {
        return dao.getCategoryStockDistribution();
    }

    public List<ArticleDto> getTopArticlesByStock(int limit) {
        return dao.findTopArticlesByStock(limit).stream()
                .map(ArticleMapper::toDto)
                .collect(Collectors.toList());
    }
}
